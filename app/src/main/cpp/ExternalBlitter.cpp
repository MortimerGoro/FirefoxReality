/* -*- Mode: C++; tab-width: 20; indent-tabs-mode: nil; c-basic-offset: 2 -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

#include "ExternalBlitter.h"
#include "vrb/ConcreteClass.h"
#include "vrb/private/ResourceGLState.h"
#include "vrb/gl.h"
#include "vrb/GLError.h"
#include "vrb/Logger.h"
#include "vrb/ShaderUtil.h"
#include <EGL/egl.h>
#include <EGL/eglext.h>

#include <unordered_map>

namespace {
const char* sVertexShader = R"SHADER(
attribute vec4 a_position;
attribute vec2 a_uv;
varying vec2 v_uv;
void main(void) {
  v_uv = a_uv;
  gl_Position = a_position;
}
)SHADER";

const char* sFragmentShader = R"SHADER(
precision mediump float;

uniform sampler2D u_texture0;

varying vec2 v_uv;

void main() {
  gl_FragColor = texture2D(u_texture0, v_uv);
}
)SHADER";

const GLfloat sVerticies[] = {
    -1.0f, 1.0f, 0.0f,
    -1.0f, -1.0f, 0.0f,
    1.0f, 1.0f, 0.0f,
    1.0f, -1.0f, 0.0f
};

}

namespace crow {

struct EGLExtensions {
    PFNEGLGETNATIVECLIENTBUFFERANDROIDPROC getNativeClientBufferANDROID { nullptr };
    PFNEGLCREATEIMAGEKHRPROC createImageKHR { nullptr };
    PFNEGLDESTROYIMAGEKHRPROC destroyImageKHR { nullptr};
    PFNGLEGLIMAGETARGETTEXTURE2DOESPROC imageTargetTexture2DOES { nullptr };

    static EGLExtensions& instance() {
      static EGLExtensions* sInstance = nullptr;
      if (!sInstance) {
        sInstance = new EGLExtensions();
      }
      return *sInstance;
    }

    bool supportsImageKHR() const {
      return getNativeClientBufferANDROID && createImageKHR && destroyImageKHR && imageTargetTexture2DOES;
    }

private:
    EGLExtensions() {
      getNativeClientBufferANDROID = reinterpret_cast<PFNEGLGETNATIVECLIENTBUFFERANDROIDPROC>(eglGetProcAddress("eglGetNativeClientBufferANDROID"));
      createImageKHR = reinterpret_cast<PFNEGLCREATEIMAGEKHRPROC>(eglGetProcAddress("eglCreateImageKHR"));
      destroyImageKHR = reinterpret_cast<PFNEGLDESTROYIMAGEKHRPROC>(eglGetProcAddress("eglDestroyImageKHR"));
      imageTargetTexture2DOES = reinterpret_cast<PFNGLEGLIMAGETARGETTEXTURE2DOESPROC>(eglGetProcAddress("glEGLImageTargetTexture2DOES"));
    }
};

struct AHardwareBufferCache;
typedef std::shared_ptr<AHardwareBufferCache> AHardwareBufferCachePtr;

struct AHardwareBufferCache {
    AHardwareBuffer* hardwareBuffer { nullptr };
    EGLClientBuffer clientBuffer { nullptr };
    EGLImageKHR image { EGL_NO_IMAGE_KHR };
    GLuint texture { 0 };

    static AHardwareBufferCachePtr create(AHardwareBuffer* hardwareBuffer) {
      auto& ext = EGLExtensions::instance();
      if (!ext.supportsImageKHR() || !hardwareBuffer) {
        return nullptr;
      }

      auto result = std::make_shared<AHardwareBufferCache>();
      result->hardwareBuffer = hardwareBuffer;
      result->clientBuffer = ext.getNativeClientBufferANDROID(hardwareBuffer);
      if (!result->clientBuffer) {
        return nullptr;
      }
      result->image = ext.createImageKHR(eglGetDisplay(EGL_DEFAULT_DISPLAY), EGL_NO_CONTEXT, EGL_NATIVE_BUFFER_ANDROID, result->clientBuffer, nullptr);
      if (result->image == EGL_NO_IMAGE_KHR) {
        return nullptr;
      }

      VRB_GL_CHECK(glGenTextures(1, &result->texture));
      if (!result->texture) {
        return nullptr;
      }
      VRB_GL_CHECK(glBindTexture(GL_TEXTURE_2D, result->texture));
      ext.imageTargetTexture2DOES(GL_TEXTURE_2D, result->image);

      return result;
    }

    ~AHardwareBufferCache() {
      auto& ext = EGLExtensions::instance();
      if (texture) {
        VRB_GL_CHECK(glDeleteTextures(1, &texture));
      }
      if (image != EGL_NO_IMAGE_KHR) {
        ext.destroyImageKHR(eglGetDisplay(EGL_DEFAULT_DISPLAY), image);
      }
      if (hardwareBuffer) {
        AHardwareBuffer_release(hardwareBuffer);
      }
    }
};

typedef std::shared_ptr<AHardwareBufferCache> AHardwareBufferCachePtr;

struct ExternalBlitter::State : public vrb::ResourceGL::State {
  GLuint vertexShader;
  GLuint fragmentShader;
  GLuint program;
  GLint aPosition;
  GLint aUV;
  GLint uTexture0;
  device::EyeRect eyes[device::EyeCount];
  AHardwareBufferCachePtr surface;
  GLfloat leftUV[8];
  GLfloat rightUV[8];
  std::unordered_map<AHardwareBuffer*, AHardwareBufferCachePtr> surfaceMap;
  State()
      : vertexShader(0)
      , fragmentShader(0)
      , program(0)
      , aPosition(0)
      , aUV(0)
      , uTexture0(0)
      , leftUV{0.0f, 1.0f, 0.0f, 0.0f, 0.5f, 1.0f, 0.5f, 0.0f}
      , rightUV{0.5f, 1.0f, 0.5f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f}
  {}
};

ExternalBlitterPtr
ExternalBlitter::Create(vrb::CreationContextPtr& aContext) {
  return std::make_shared<vrb::ConcreteClass<ExternalBlitter, ExternalBlitter::State> >(aContext);
}

void
ExternalBlitter::StartFrame(AHardwareBuffer* buffer, const device::EyeRect& aLeftEye, const device::EyeRect& aRightEye) {
  auto it = m.surfaceMap.find(buffer);

  if (it == m.surfaceMap.end()) {
    VRB_LOG("Creating ImageKHR for AHardwareBuffer: %p", buffer);
    m.surface = AHardwareBufferCache::create(buffer);
    m.surfaceMap[buffer] = m.surface;
  } else {
    m.surface = it->second;
  }

  if (!m.surface) {
    VRB_ERROR("Failed to find ImageKHR for AHardwareBuffer: %p", buffer);
    return;
  }

  m.eyes[device::EyeIndex(device::Eye::Left)] = aLeftEye;
  m.eyes[device::EyeIndex(device::Eye::Right)] = aRightEye;
}

void
ExternalBlitter::Draw(const device::Eye aEye) {
  if (!m.program || !m.surface) {
    VRB_ERROR("ExternalBlitter::Draw FAILED!");
    return;
  }
  const GLboolean enabled = glIsEnabled(GL_DEPTH_TEST);
  if (enabled) {
    VRB_GL_CHECK(glDisable(GL_DEPTH_TEST));
  }
  VRB_GL_CHECK(glUseProgram(m.program));
  VRB_GL_CHECK(glActiveTexture(GL_TEXTURE0));
  VRB_GL_CHECK(glBindTexture(GL_TEXTURE_2D, m.surface->texture));
  //m.defaultT->Bind();
  VRB_GL_CHECK(glUniform1i(m.uTexture0, 0));
  VRB_GL_CHECK(glVertexAttribPointer((GLuint)m.aPosition, 3, GL_FLOAT, GL_FALSE, 0, sVerticies));
  VRB_GL_CHECK(glEnableVertexAttribArray((GLuint)m.aPosition));
  GLfloat* data = (aEye == device::Eye::Left ? &m.leftUV[0] : &m.rightUV[0]);
  VRB_GL_CHECK(glVertexAttribPointer((GLuint)m.aUV, 2, GL_FLOAT, GL_FALSE, 0, data));
  VRB_GL_CHECK(glEnableVertexAttribArray((GLuint)m.aUV));
  VRB_GL_CHECK(glDrawArrays(GL_TRIANGLE_STRIP, 0, 4));
  if (enabled) {
    VRB_GL_CHECK(glEnable(GL_DEPTH_TEST));
  }
}

void
ExternalBlitter::EndFrame() {
  m.surface = nullptr;
}

void
ExternalBlitter::StopPresenting() {
  m.surface = nullptr;
  m.surfaceMap.clear();
}

void
ExternalBlitter::CancelFrame(AHardwareBuffer*) {

}

ExternalBlitter::ExternalBlitter(State& aState, vrb::CreationContextPtr& aContext)
    : vrb::ResourceGL(aState, aContext)
    , m(aState)
{}

void
ExternalBlitter::InitializeGL() {
  m.vertexShader = vrb::LoadShader(GL_VERTEX_SHADER, sVertexShader);
  m.fragmentShader = vrb::LoadShader(GL_FRAGMENT_SHADER, sFragmentShader);
  if (m.vertexShader && m.fragmentShader) {
    m.program = vrb::CreateProgram(m.vertexShader, m.fragmentShader);
  }
  if (m.program) {
    m.aPosition = vrb::GetAttributeLocation(m.program, "a_position");
    m.aUV = vrb::GetAttributeLocation(m.program, "a_uv");
    m.uTexture0 = vrb::GetUniformLocation(m.program, "u_texture0");
  }
}

void
ExternalBlitter::ShutdownGL() {
  if (m.program) {
    VRB_GL_CHECK(glDeleteProgram(m.program));
    m.program = 0;
  }
  if (m.vertexShader) {
    VRB_GL_CHECK(glDeleteShader(m.vertexShader));
    m.vertexShader = 0;
  }
  if (m.vertexShader) {
    VRB_GL_CHECK(glDeleteShader(m.fragmentShader));
    m.fragmentShader = 0;
  }
}

} // namespace crow
