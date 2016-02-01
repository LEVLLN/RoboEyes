#include <jni.h>

JNIEXPORT jstring JNICALL
Java_lk_simplecode_kz_mycvapplication_MainActivity_getStringFromNative(JNIEnv *env,
                                                                       jobject instance) {

    // TODO


    return (*env)->NewStringUTF(env, "CHECK CHECK CHECK");
}