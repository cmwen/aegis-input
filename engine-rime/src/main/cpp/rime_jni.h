#ifndef AEGISINPUT_RIME_JNI_H
#define AEGISINPUT_RIME_JNI_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL
Java_com_aegisinput_engine_RimeBridge_nativeInitialize(JNIEnv *env, jobject thiz,
                                                       jstring data_dir, jstring shared_dir);

JNIEXPORT void JNICALL
Java_com_aegisinput_engine_RimeBridge_nativeShutdown(JNIEnv *env, jobject thiz);

JNIEXPORT jlong JNICALL
Java_com_aegisinput_engine_RimeBridge_nativeCreateSession(JNIEnv *env, jobject thiz);

JNIEXPORT void JNICALL
Java_com_aegisinput_engine_RimeBridge_nativeDestroySession(JNIEnv *env, jobject thiz,
                                                           jlong session_id);

JNIEXPORT jboolean JNICALL
Java_com_aegisinput_engine_RimeBridge_nativeProcessKey(JNIEnv *env, jobject thiz,
                                                       jlong session_id, jstring key);

JNIEXPORT jstring JNICALL
Java_com_aegisinput_engine_RimeBridge_nativeGetComposingText(JNIEnv *env, jobject thiz,
                                                              jlong session_id);

JNIEXPORT jobjectArray JNICALL
Java_com_aegisinput_engine_RimeBridge_nativeGetCandidates(JNIEnv *env, jobject thiz,
                                                          jlong session_id);

JNIEXPORT jstring JNICALL
Java_com_aegisinput_engine_RimeBridge_nativeCommitComposition(JNIEnv *env, jobject thiz,
                                                              jlong session_id);

JNIEXPORT void JNICALL
Java_com_aegisinput_engine_RimeBridge_nativeClearComposition(JNIEnv *env, jobject thiz,
                                                             jlong session_id);

#ifdef __cplusplus
}
#endif

#endif // AEGISINPUT_RIME_JNI_H
