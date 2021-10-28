#pragma once

#include "ControllerDelegate.h"
#include <array>
#include <vector>
#include <optional>

namespace crow {
    // Helper template to iterate over enums
    template <class T>
    struct EnumRange {
        struct Iterator {
            explicit Iterator(int v) : value(v) {}
            void operator++() { ++value; }
            bool operator!=(Iterator rhs) { return value != rhs.value; }
            T operator*() const { return static_cast<T>(value); }

            int value = 0;
        };

        Iterator begin() const { return Iterator(0); }
        Iterator end() const { return Iterator(static_cast<int>(T::enum_count)); }
    };

#ifdef HVR
    // Workaround for SDK bug
    constexpr const char* kPathLeftHand { "/user/hand/right" };
    constexpr const char* kPathRightHand { "/user/hand/left" };
#else
    constexpr const char* kPathLeftHand { "/user/hand/left" };
    constexpr const char* kPathRightHand { "/user/hand/right" };
#endif
    constexpr const char* kPathGripPose { "input/grip/pose" };
    constexpr const char* kPathAimPose { "input/aim/pose" };
    constexpr const char* kPathTrigger { "input/trigger" };
    constexpr const char* kPathSqueeze { "input/squeeze" };
    constexpr const char* kPathThumbstick { "input/thumbstick" };
    constexpr const char* kPathThumbrest { "input/thumbrest" };
    constexpr const char* kPathTrackpad { "input/trackpad" };
    constexpr const char* kPathSelect { "/input/select" };
    constexpr const char* kPathMenu { "input/menu" };
    constexpr const char* kPathButtonA { "input/a" };
    constexpr const char* kPathButtonB { "input/b" };
    constexpr const char* kPathButtonX { "input/x" };
    constexpr const char* kPathButtonY { "input/y" };
    constexpr const char* kPathActionClick { "click" };
    constexpr const char* kPathActionTouch { "touch" };
    constexpr const char* kPathActionValue { "value" };

    // OpenXR Button List
    enum class OpenXRButtonType {
        Trigger, Squeeze, Menu, Back, Trackpad, Thumbstick, Thumbrest, ButtonA, ButtonB, ButtonX, ButtonY, enum_count
    };
    constexpr std::array<const char*, 11> OpenXRButtonTypeNames[] = {
        "trigger", "squeeze", "menu", "back", "trackpad", "thumbstick", "thumbrest", "a", "b", "x", "y"
    };
    using OpenXRButtonTypes = EnumRange<OpenXRButtonType>;

    static_assert(OpenXRButtonTypeNames->size() == static_cast<int>(OpenXRButtonType::enum_count), "sizes don't match");

    // OpenXR Axis List
    enum class OpenXRAxisType {
        Trackpad, Thumbstick, TrackpadX, TrackpadY, ThumbstickX, ThumbstickY, enum_count
    };

    constexpr std::array<const char*, 6> OpenXRAxisTypeNames[] = {
        "trackpad", "thumbstick", "trackpad_x", "trackpad_y", "thumbstick_x", "thumbstick_y"
    };
    using OpenXRAxisTypes = EnumRange<OpenXRAxisType>;
    static_assert(OpenXRAxisTypeNames->size() == static_cast<int>(OpenXRAxisType::enum_count), "sizes don't match");

    // Mapping Classes
    using OpenXRInputProfile = const char* const;
    using OpenXRButtonPath = const char* const;

    enum OpenXRButtonFlags {
        Click = (1u << 0),
        Touch = (1u << 1),
        Value  = (1u << 2),
        ValueTouch = Touch | Value,
        ClickTouch = Click | Touch,
        ClickValue = Click | Value,
        All  = Click | Touch | Value,
    };

    enum OpenXRHandFlags {
        Left = (1u << 0),
        Right = (1u << 1),
        Both = Left | Right
    };

    inline OpenXRButtonFlags operator|(OpenXRButtonFlags a, OpenXRButtonFlags b) {
        return static_cast<OpenXRButtonFlags>(static_cast<int>(a) | static_cast<int>(b));
    }

    inline OpenXRHandFlags operator|(OpenXRHandFlags a, OpenXRHandFlags b) {
        return static_cast<OpenXRHandFlags>(static_cast<int>(a) | static_cast<int>(b));
    }

    struct OpenXRButton {
        OpenXRButtonType type;
        OpenXRButtonPath path;
        OpenXRButtonFlags flags;
        OpenXRHandFlags hand;
        std::optional<ControllerDelegate::Button> browserMapping;
        bool reserved { false };
    };

    struct OpenXRAxis {
        OpenXRAxisType type;
        OpenXRButtonPath path;
        OpenXRHandFlags hand;
    };

    struct OpenXRInputMapping {
        const char* const path { nullptr };
        const char* const systemFilter {nullptr };
        const char* const leftControllerModel { nullptr };
        const char* const rightControllerModel { nullptr };
        device::DeviceType controllerType { device::OculusQuest };
        std::vector<OpenXRInputProfile> profiles;
        std::vector<OpenXRButton> buttons;
        std::vector<OpenXRAxis> axes;
    };

    /*
     * Mappings
     */

    // Oculus Touch:  https://github.com/immersive-web/webxr-input-profiles/blob/master/packages/registry/profiles/oculus/oculus-touch-v2.json
    const OpenXRInputMapping OculusTouch {
        "/interaction_profiles/oculus/touch_controller",
        "Oculus Quest",
        "vr_controller_oculusquest_left.obj",
        "vr_controller_oculusquest_right.obj",
        device::OculusQuest,
        std::vector<OpenXRInputProfile> { "oculus-touch-v2", "oculus-touch", "generic-trigger-squeeze-thumbstick" },
        std::vector<OpenXRButton> {
            { OpenXRButtonType::Trigger, kPathTrigger, OpenXRButtonFlags::ValueTouch, OpenXRHandFlags::Both },
            { OpenXRButtonType::Squeeze, kPathSqueeze, OpenXRButtonFlags::Value, OpenXRHandFlags::Both },
            { OpenXRButtonType::Thumbstick, kPathThumbstick, OpenXRButtonFlags::ClickTouch, OpenXRHandFlags::Both },
            { OpenXRButtonType::ButtonA, kPathButtonX, OpenXRButtonFlags::ClickTouch, OpenXRHandFlags::Left },
            { OpenXRButtonType::ButtonB, kPathButtonY, OpenXRButtonFlags::ClickTouch, OpenXRHandFlags::Left, ControllerDelegate::Button::BUTTON_APP },
            { OpenXRButtonType::ButtonX, kPathButtonA, OpenXRButtonFlags::ClickTouch, OpenXRHandFlags::Right },
            { OpenXRButtonType::ButtonY, kPathButtonB, OpenXRButtonFlags::ClickTouch, OpenXRHandFlags::Right, ControllerDelegate::Button::BUTTON_APP },
            { OpenXRButtonType::Thumbrest, kPathThumbrest, OpenXRButtonFlags::Touch, OpenXRHandFlags::Both },
            { OpenXRButtonType::Menu, kPathMenu, OpenXRButtonFlags::Click, OpenXRHandFlags::Left, std::nullopt, true }
        },
        std::vector<OpenXRAxis> {
            { OpenXRAxisType::Thumbstick, kPathThumbstick,  OpenXRHandFlags::Both },
        }
    };

    // Oculus Touch2:  https://github.com/immersive-web/webxr-input-profiles/blob/master/packages/registry/profiles/oculus/oculus-touch-v3.json
    const OpenXRInputMapping OculusTouch2 {
            "/interaction_profiles/oculus/touch_controller",
            "Oculus Quest2",
            "vr_controller_oculusquest_left.obj",
            "vr_controller_oculusquest_right.obj",
            device::OculusQuest,
            std::vector<OpenXRInputProfile> { "oculus-touch-v3", "oculus-touch-v2", "oculus-touch", "generic-trigger-squeeze-thumbstick" },
            std::vector<OpenXRButton> {
                    { OpenXRButtonType::Trigger, kPathTrigger, OpenXRButtonFlags::ValueTouch, OpenXRHandFlags::Both },
                    { OpenXRButtonType::Squeeze, kPathSqueeze, OpenXRButtonFlags::Value, OpenXRHandFlags::Both },
                    { OpenXRButtonType::Thumbstick, kPathThumbstick, OpenXRButtonFlags::ClickTouch, OpenXRHandFlags::Both },
                    { OpenXRButtonType::ButtonA, kPathButtonX, OpenXRButtonFlags::ClickTouch, OpenXRHandFlags::Left },
                    { OpenXRButtonType::ButtonB, kPathButtonY, OpenXRButtonFlags::ClickTouch, OpenXRHandFlags::Left, ControllerDelegate::Button::BUTTON_APP },
                    { OpenXRButtonType::ButtonX, kPathButtonA, OpenXRButtonFlags::ClickTouch, OpenXRHandFlags::Right },
                    { OpenXRButtonType::ButtonY, kPathButtonB, OpenXRButtonFlags::ClickTouch, OpenXRHandFlags::Right, ControllerDelegate::Button::BUTTON_APP },
                    { OpenXRButtonType::Thumbrest, kPathThumbrest, OpenXRButtonFlags::Touch, OpenXRHandFlags::Both },
                    { OpenXRButtonType::Menu, kPathMenu, OpenXRButtonFlags::Click, OpenXRHandFlags::Left, std::nullopt, true }
            },
            std::vector<OpenXRAxis> {
                    { OpenXRAxisType::Thumbstick, kPathThumbstick,  OpenXRHandFlags::Both },
            }
    };
    
    // HVR 3DOF: https://github.com/immersive-web/webxr-input-profiles/blob/master/packages/registry/profiles/generic/generic-trigger-touchpad.json
    const OpenXRInputMapping Hvr3DOF {
            "/interaction_profiles/huawei/controller",
            "Haliday: G3HMD by Huawei",
            nullptr,
            "vr_controller_focus.obj",
            device::ViveFocus,
            std::vector<OpenXRInputProfile> { "generic-trigger-touchpad" },
            std::vector<OpenXRButton> {
                    { OpenXRButtonType::Trigger, kPathTrigger, OpenXRButtonFlags::ValueTouch, OpenXRHandFlags::Both },
                    { OpenXRButtonType::Trackpad, kPathTrackpad, OpenXRButtonFlags::All, OpenXRHandFlags::Both },
            },
            std::vector<OpenXRAxis> {
                { OpenXRAxisType::TrackpadX, "input/trackpad/x",  OpenXRHandFlags::Both },
                { OpenXRAxisType::TrackpadY, "input/trackpad/y",  OpenXRHandFlags::Both }
            }
    };

  const OpenXRInputMapping Hvr6DOF {
      "/interaction_profiles/huawei/controller",
      "Haliday: G3HMD by Huawei",
      "hvr_6dof_left.obj",
      "hvr_6dof_right.obj",
      device::OculusQuest,
      std::vector<OpenXRInputProfile> { "oculus-touch-v3", "oculus-touch-v2", "oculus-touch", "generic-trigger-squeeze-thumbstick" },
      std::vector<OpenXRButton> {
          { OpenXRButtonType::Trigger, kPathTrigger, OpenXRButtonFlags::ClickValue, OpenXRHandFlags::Both },
          { OpenXRButtonType::Thumbstick, kPathThumbstick, OpenXRButtonFlags::Click, OpenXRHandFlags::Both },
          { OpenXRButtonType::ButtonX, "input/ax", OpenXRButtonFlags::Click, OpenXRHandFlags::Both },
          { OpenXRButtonType::ButtonY, "input/by", OpenXRButtonFlags::Click, OpenXRHandFlags::Both, ControllerDelegate::Button::BUTTON_APP },
          { OpenXRButtonType::Thumbrest, "input/grip", OpenXRButtonFlags::ClickValue, OpenXRHandFlags::Both },
      },
      std::vector<OpenXRAxis> {
          { OpenXRAxisType::ThumbstickX, "input/trackpad/x",  OpenXRHandFlags::Both },
          { OpenXRAxisType::ThumbstickY, "input/trackpad/y",  OpenXRHandFlags::Both }
      }
  };

    // Default fallback: https://github.com/immersive-web/webxr-input-profiles/blob/master/packages/registry/profiles/generic/generic-button.json
    const OpenXRInputMapping KHRSimple {
            "/interaction_profiles/khr/simple_controller",
            nullptr,
            "vr_controller_oculusgo.obj",
            "vr_controller_oculusgo.obj",
            device::OculusGo,
            std::vector<OpenXRInputProfile> { "generic-button" },
            std::vector<OpenXRButton> {
                    { OpenXRButtonType::Trigger, kPathTrigger, OpenXRButtonFlags::Click, OpenXRHandFlags::Both },
            },
            {}
    };

#if defined(HVR_6DOF)
#define HVR_MAPPING Hvr6DOF
#else
#define HVR_MAPPING Hvr3DOF
#endif

    const std::array<OpenXRInputMapping, 4> OpenXRInputMappings {
        OculusTouch, OculusTouch2, HVR_MAPPING, KHRSimple
    };

} // namespace crow
