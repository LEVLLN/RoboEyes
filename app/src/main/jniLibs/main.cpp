//
// Created by user on 27.01.2016.
//

#include "main.h"

class Main {
    JNIEXPORT jstring;

    JNICALL
    Java_lk_simplecode_kz_mycvapplication_MainActivity_getStringFromNative
            (JNIenv *env, jobject obj) {
#ifdef _INTEL_COMPILER_UPDATE
        return (*env)->NewStringUTF(env,"Hello from Intel C++");
#else
        return (*env)->NewStringUTF(env, "Hello from default C++");
#endif
    }
};