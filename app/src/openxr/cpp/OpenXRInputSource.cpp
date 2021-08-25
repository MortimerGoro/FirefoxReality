#include "OpenXRInputSource.h"

namespace crow {

OpenXRInputSourcePtr OpenXRInputSource::Create(XrInstance instance, XrSession session, const XrSystemProperties& properties, OpenXRHandFlags handeness, int index)
{
    OpenXRInputSourcePtr input(new OpenXRInputSource(instance, session, properties, handeness, index));
    if (XR_FAILED(input->Initialize()))
        return nullptr;
    return input;
}

OpenXRInputSource::OpenXRInputSource(XrInstance instance, XrSession session, const XrSystemProperties& properties, OpenXRHandFlags handeness, int index)
    : mInstance(instance)
    , mSession(session)
    , mSystemProperties(properties)
    , mHandeness(handeness)
    , mIndex(index)
{
}

OpenXRInputSource::~OpenXRInputSource()
{
    if (mActionSet != XR_NULL_HANDLE)
        xrDestroyActionSet(mActionSet);
    if (mGripSpace != XR_NULL_HANDLE)
        xrDestroySpace(mGripSpace);
    if (mPointerSpace != XR_NULL_HANDLE)
        xrDestroySpace(mPointerSpace);
}

XrResult OpenXRInputSource::Initialize()
{
    mSubactionPathName = mHandeness == OpenXRHandFlags::Left ? kPathLeftHand : kPathRightHand;
    RETURN_IF_XR_FAILED(xrStringToPath(mInstance, mSubactionPathName.c_str(), &mSubactionPath));

    // Initialize Action Set.
    std::string prefix = std::string("input_") + (mHandeness == OpenXRHandFlags::Left ? "left" : "right");
    std::string actionSetName = prefix + "_action_set";
    XrActionSetCreateInfo createInfo { XR_TYPE_ACTION_SET_CREATE_INFO };
    std::strncpy(createInfo.actionSetName, actionSetName.c_str(), XR_MAX_ACTION_SET_NAME_SIZE - 1);
    std::strncpy(createInfo.localizedActionSetName, actionSetName.c_str(), XR_MAX_ACTION_SET_NAME_SIZE - 1);

    RETURN_IF_XR_FAILED(xrCreateActionSet(mInstance, &createInfo, &mActionSet), mInstance);

    // Initialize pose actions and spaces.
    RETURN_IF_XR_FAILED(CreateAction(XR_ACTION_TYPE_POSE_INPUT, prefix + "_grip", mGripAction));
    RETURN_IF_XR_FAILED(CreateSpaceAction(mGripAction, mGripSpace), mInstance);
    RETURN_IF_XR_FAILED(CreateAction(XR_ACTION_TYPE_POSE_INPUT, prefix + "_pointer", mPointerAction));
    RETURN_IF_XR_FAILED(CreateSpaceAction(mPointerAction, mPointerSpace));

    // Initialize button actions.
    for (auto buttonType : OpenXRButtonTypes()) {
        OpenXRButtonActions actions;
        CreateButtonActions(buttonType, prefix, actions);
        mButtonActions.emplace(buttonType, actions);
    }

    // Initialize axes.
    for (auto axisType : OpenXRAxisTypes()) {
        XrAction axisAction = XR_NULL_HANDLE;
        std::string name = prefix + "_axis_" + OpenXRAxisTypeNames->at(static_cast<int>(axisType));
        RETURN_IF_XR_FAILED(CreateAction(XR_ACTION_TYPE_VECTOR2F_INPUT, name, axisAction), mInstance, false);
        mAxisActions.emplace(axisType, axisAction);
    }

    // Filter mappings
    for (auto& mapping: OpenXRInputMappings) {
        if (mapping.systemFilter && strcmp(mapping.systemFilter, mSystemProperties.systemName) != 0) {
            continue;
        }
        mMappings.push_back(mapping);
    }

    return XR_SUCCESS;
}

XrResult OpenXRInputSource::CreateSpaceAction(XrAction action, XrSpace& space) const
{
    XrActionSpaceCreateInfo createInfo { XR_TYPE_ACTION_SPACE_CREATE_INFO };
    createInfo.action = action;
    createInfo.subactionPath = mSubactionPath;
    createInfo.poseInActionSpace = XrPoseIdentity();

    return xrCreateActionSpace(mSession, &createInfo, &space);
}

XrResult OpenXRInputSource::CreateAction(XrActionType actionType, const std::string& name, XrAction& action) const
{
    XrActionCreateInfo createInfo { XR_TYPE_ACTION_CREATE_INFO };
    createInfo.actionType = actionType;
    createInfo.countSubactionPaths = 1;
    createInfo.subactionPaths = &mSubactionPath;
    std::strncpy(createInfo.actionName, name.c_str(), XR_MAX_ACTION_SET_NAME_SIZE - 1);
    std::strncpy(createInfo.localizedActionName, name.c_str(), XR_MAX_ACTION_SET_NAME_SIZE - 1);

    return xrCreateAction(mActionSet, &createInfo, &action);
}

XrResult OpenXRInputSource::CreateButtonActions(OpenXRButtonType type, const std::string& prefix, OpenXRButtonActions& actions) const
{
    auto name = prefix + "_button_" + OpenXRButtonTypeNames->at(static_cast<int>(type));

    RETURN_IF_XR_FAILED(CreateAction(XR_ACTION_TYPE_BOOLEAN_INPUT, name + "_click", actions.click));
    RETURN_IF_XR_FAILED(CreateAction(XR_ACTION_TYPE_BOOLEAN_INPUT, name + "_touch", actions.touch));
    RETURN_IF_XR_FAILED(CreateAction(XR_ACTION_TYPE_FLOAT_INPUT, name + "_value", actions.value));

    return XR_SUCCESS;
}

XrResult OpenXRInputSource::CreateBinding(const char* profilePath, XrAction action, const std::string & bindingPath, SuggestedBindings& bindings) const
{
    assert(profilePath != XR_NULL_PATH);
    assert(action != XR_NULL_HANDLE);
    assert(!bindingPath.empty());

    XrPath path = XR_NULL_PATH;
    RETURN_IF_XR_FAILED(xrStringToPath(mInstance, bindingPath.c_str(), &path));

    XrActionSuggestedBinding binding { action, path };
    if (auto it = bindings.find(profilePath); it != bindings.end()) {
        it->second.push_back(binding);
    }
    else {
        bindings.emplace(profilePath, std::vector<XrActionSuggestedBinding>{ binding });
    }

    return XR_SUCCESS;
}

XrResult OpenXRInputSource::GetPose(XrSpace space, XrSpace baseSpace, const XrFrameState& frameState, vrb::Matrix& pose, bool& isPositionEmulated) const
{
    XrSpaceLocation location { XR_TYPE_SPACE_LOCATION };
    RETURN_IF_XR_FAILED(xrLocateSpace(space, baseSpace, frameState.predictedDisplayTime, &location));

    if (location.locationFlags & XR_SPACE_LOCATION_ORIENTATION_TRACKED_BIT)
        pose = XrPoseToMatrix(location.pose);
    isPositionEmulated = !(location.locationFlags & XR_SPACE_LOCATION_POSITION_VALID_BIT);

    return XR_SUCCESS;
}

std::optional<OpenXRInputSource::OpenXRButtonState> OpenXRInputSource::GetButtonState(OpenXRButtonType buttonType) const
{
    auto it = mButtonActions.find(buttonType);
    if (it == mButtonActions.end())
        return std::nullopt;

    OpenXRButtonState result;
    bool hasValue = false;
    auto& actions = it->second;

    auto queryActionState = [this, &hasValue](XrAction action, auto& value, auto defaultValue) {
        if (action != XR_NULL_HANDLE && XR_SUCCEEDED(this->GetActionState(action, &value)))
            hasValue = true;
        else
            value = defaultValue;
    };

    queryActionState(actions.click, result.clicked, false);
    queryActionState(actions.touch, result.touched, result.clicked);
    queryActionState(actions.value, result.value, result.clicked ? 1.0 : 0.0);

    return hasValue ? std::make_optional(result) : std::nullopt;
}

std::optional<XrVector2f> OpenXRInputSource::GetAxis(OpenXRAxisType axisType) const
{
    auto it = mAxisActions.find(axisType);
    if (it == mAxisActions.end())
        return std::nullopt;

    XrVector2f axis;
    if (XR_FAILED(GetActionState(it->second, &axis)))
        return std::nullopt;

    return axis;
}

XrResult OpenXRInputSource::GetActionState(XrAction action, bool* value) const
{
    assert(value);
    assert(action != XR_NULL_HANDLE);

    XrActionStateBoolean state { XR_TYPE_ACTION_STATE_BOOLEAN };
    XrActionStateGetInfo info { XR_TYPE_ACTION_STATE_GET_INFO };
    info.action = action;

    RETURN_IF_XR_FAILED(xrGetActionStateBoolean(mSession, &info, &state), mInstance);
    *value = state.currentState;

    return XR_SUCCESS;
}

XrResult OpenXRInputSource::GetActionState(XrAction action, float* value) const
{
    assert(value);
    assert(action != XR_NULL_HANDLE);

    XrActionStateFloat state { XR_TYPE_ACTION_STATE_FLOAT };
    XrActionStateGetInfo info { XR_TYPE_ACTION_STATE_GET_INFO };
    info.action = action;

    RETURN_IF_XR_FAILED(xrGetActionStateFloat(mSession, &info, &state), mInstance);
    *value = state.currentState;

    return XR_SUCCESS;
}

XrResult OpenXRInputSource::GetActionState(XrAction action, XrVector2f* value) const
{
    assert(value);
    assert(action != XR_NULL_HANDLE);

    XrActionStateVector2f state { XR_TYPE_ACTION_STATE_VECTOR2F };
    XrActionStateGetInfo info { XR_TYPE_ACTION_STATE_GET_INFO };
    info.action = action;

    RETURN_IF_XR_FAILED(xrGetActionStateVector2f(mSession, &info, &state), mInstance);
    *value = state.currentState;

    return XR_SUCCESS;
}


XrResult OpenXRInputSource::SuggestBindings(SuggestedBindings& bindings) const
{
    for (auto& mapping : mMappings) {
        // Suggest binding for pose actions.
        RETURN_IF_XR_FAILED(CreateBinding(mapping.path, mGripAction, mSubactionPathName + kPathGripPose, bindings), mInstance);
        RETURN_IF_XR_FAILED(CreateBinding(mapping.path, mPointerAction, mSubactionPathName + kPathAimPose, bindings), mInstance);

        // Suggest binding for button actions.
        for (auto& button: mapping.buttons) {
            if ((button.hand & mHandeness) == 0) {
                continue;
            }

            auto it = mButtonActions.find(button.type);
            if (it == mButtonActions.end()) {
                continue;
            }
            const auto& actions = it->second;
            if (button.flags & OpenXRButtonFlags::Click) {
                assert(actions.click != XR_NULL_HANDLE);
                RETURN_IF_XR_FAILED(CreateBinding(mapping.path, actions.click, mSubactionPathName + button.path +  "/" + kPathActionClick, bindings));
            }
            if (button.flags & OpenXRButtonFlags::Touch) {
                assert(actions.touch != XR_NULL_HANDLE);
                RETURN_IF_XR_FAILED(CreateBinding(mapping.path, actions.touch, mSubactionPathName + button.path + "/" + kPathActionTouch, bindings));
            }
            if (button.flags & OpenXRButtonFlags::Value) {
                assert(actions.value != XR_NULL_HANDLE);
                RETURN_IF_XR_FAILED(CreateBinding(mapping.path, actions.value, mSubactionPathName + button.path + "/" + kPathActionValue, bindings));
            }
        }

        // Suggest binding for axis actions.
        for (auto& axis: mapping.axes) {
            auto it = mAxisActions.find(axis.type);
            if (it == mAxisActions.end()) {
                continue;
            }
            auto action = it->second;
            assert(action != XR_NULL_HANDLE);
            RETURN_IF_XR_FAILED(CreateBinding(mapping.path, action, mSubactionPathName + axis.path, bindings));
        }
    }

    return XR_SUCCESS;
}

void OpenXRInputSource::Update(XrSpace localSpace, const XrFrameState& frameState, ControllerDelegate& delegate)
{
    if (!mActiveMapping) {
        delegate.SetEnabled(mIndex, false);
        return;
    }

    delegate.SetLeftHanded(mIndex, mHandeness == OpenXRHandFlags::Left);
    delegate.SetTargetRayMode(mIndex, device::TargetRayMode::TrackedPointer);

    // Pose transforms.
    vrb::Matrix pointerOrigin;
    bool positionEmulated { false };
    if (XR_FAILED(GetPose(mPointerSpace, localSpace, frameState, pointerOrigin, positionEmulated))) {
        delegate.SetEnabled(mIndex, false);
        return;
    }
    delegate.SetEnabled(mIndex, true);
    delegate.SetTransform(mIndex, pointerOrigin);

    vrb::Matrix gripPose;
    if (XR_SUCCEEDED(GetPose(mGripSpace, localSpace, frameState, gripPose, positionEmulated))) {
        delegate.SetImmersiveBeamTransform(mIndex, gripPose);
    } else {
        delegate.SetImmersiveBeamTransform(mIndex, vrb::Matrix::Identity());
    }

    // Buttons.
    int buttonCount { 0 };
    for (auto& button: mActiveMapping->buttons) {
        if ((button.hand & mHandeness) == 0) {
            continue;
        }
        auto state = GetButtonState(button.type);
        if (!state.has_value()) {
            VRB_ERROR("Cant read button type with path '%s'", button.path);
            continue;
        }

        buttonCount++;
        delegate.SetButtonState(mIndex, -1, device::kImmersiveButtonB, state->clicked, state->touched, state->value);


    }
    delegate.SetButtonCount(mIndex, buttonCount);


    Vector<std::optional<Device::FrameData::InputSourceButton>> buttons;
    for (auto& type : openXRButtonTypes)
        buttons.append(getButton(type));

    // Trigger is mandatory in xr-standard mapping.
    if (buttons.isEmpty() || !buttons.first().has_value())
        return std::nullopt;

    for (size_t i = 0; i < buttons.size(); ++i) {
        if (buttons[i]) {
            data.buttons.append(*buttons[i]);
            continue;
        }
        // Add placeholder if there are more valid buttons in the list.
        for (size_t j = i + 1; j < buttons.size(); ++j) {
            if (buttons[j]) {
                data.buttons.append({ });
                break;
            }
        }
    }

    // Axes.
    std::vector<std::optional<XrVector2f>> axes;
    for (auto type : OpenXRAxisTypes())
        axes.append(getAxis(type));

    for (size_t i = 0; i < axes.size(); ++i) {
        if (axes[i]) {
            data.axes.append(axes[i]->x);
            data.axes.append(axes[i]->y);
            continue;
        }
        // Add placeholder if there are more valid axes in the list.
        for (size_t j = i + 1; j < buttons.size(); ++j) {
            if (axes[j]) {
                data.axes.append(0.0f);
                data.axes.append(0.0f);
                break;
            }
        }
    }

    return data;
}

XrResult OpenXRInputSource::UpdateInteractionProfile()
{
    XrInteractionProfileState state { XR_TYPE_INTERACTION_PROFILE_STATE };
    RETURN_IF_XR_FAILED(xrGetCurrentInteractionProfile(mSession, mSubactionPath, &state));

    constexpr uint32_t bufferSize = 100;
    char buffer[bufferSize];
    uint32_t writtenCount = 0;
    RETURN_IF_XR_FAILED(xrPathToString(mInstance, state.interactionProfile, bufferSize, &writtenCount, buffer));

    mActiveMapping = nullptr;

    for (auto& mapping : mMappings) {
        if (!strncmp(mapping.path, buffer, writtenCount)) {
            mActiveMapping = &mapping;
            break;
        }
    }

    return XR_SUCCESS;
}

} // namespace crow
