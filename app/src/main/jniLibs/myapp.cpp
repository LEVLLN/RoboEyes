#include <android/log.h>
#include <string>
#include "myapp.h"

JNIEXPORT jstring JNICALL Java_lk_simplecode_kz_mycvapplication_MainActivity_stringFromJNI(JNIEnv* env, jclass clazz)
{
    std::string tag("GREETING");
    std::string message("Hello from C++!");
    __android_log_print(ANDROID_LOG_INFO, tag.c_str(), "%s", message.c_str());
    std::string jniMessage("Hello from JNI!");
    return env->NewStringUTF(jniMessage.c_str());
}