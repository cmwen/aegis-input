#include "rime_jni.h"
#include "pinyin_engine.h"
#include <codecvt>
#include <locale>
#include <mutex>
#include <unordered_map>

namespace {
std::mutex engineMutex;
aegis::PinyinEngine engine;
std::unordered_map<jlong, aegis::PinyinSession> sessions;
jlong nextSession = 1;
bool initialized = false;
jstring javaString(JNIEnv* env, const std::string& value) {
    std::wstring_convert<std::codecvt_utf8_utf16<char16_t>, char16_t> converter;
    const auto utf16 = converter.from_bytes(value);
    return env->NewString(reinterpret_cast<const jchar*>(utf16.data()), static_cast<jsize>(utf16.size()));
}
std::string stringValue(JNIEnv* env, jstring value) {
    const char* chars = env->GetStringUTFChars(value, nullptr);
    if (!chars) return "";
    std::string result(chars);
    env->ReleaseStringUTFChars(value, chars);
    return result;
}
}
extern "C" {
JNIEXPORT void JNICALL Java_com_aegisinput_engine_RimeBridge_nativeInitialize(
        JNIEnv* env, jobject, jstring dataDir, jstring) {
    std::lock_guard<std::mutex> lock(engineMutex);
    try {
        engine.load(stringValue(env, dataDir) + "/pinyin_simp.dict.yaml");
        initialized = true;
    } catch (const std::exception& error) {
        initialized = false;
        env->ThrowNew(env->FindClass("java/lang/IllegalStateException"), error.what());
    }
}
JNIEXPORT void JNICALL Java_com_aegisinput_engine_RimeBridge_nativeShutdown(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lock(engineMutex);
    sessions.clear(); engine = aegis::PinyinEngine{}; initialized = false;
}
JNIEXPORT jlong JNICALL Java_com_aegisinput_engine_RimeBridge_nativeCreateSession(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lock(engineMutex);
    if (!initialized) return 0;
    const auto id = nextSession++;
    sessions.emplace(id, aegis::PinyinSession{});
    return id;
}
JNIEXPORT void JNICALL Java_com_aegisinput_engine_RimeBridge_nativeDestroySession(JNIEnv*, jobject, jlong id) {
    std::lock_guard<std::mutex> lock(engineMutex); sessions.erase(id);
}
JNIEXPORT jboolean JNICALL Java_com_aegisinput_engine_RimeBridge_nativeProcessKey(
        JNIEnv* env, jobject, jlong id, jstring key) {
    std::lock_guard<std::mutex> lock(engineMutex);
    auto it = sessions.find(id);
    return it != sessions.end() && it->second.process(stringValue(env, key), engine);
}
JNIEXPORT jstring JNICALL Java_com_aegisinput_engine_RimeBridge_nativeGetComposingText(JNIEnv* env, jobject, jlong id) {
    std::lock_guard<std::mutex> lock(engineMutex);
    auto it = sessions.find(id);
    return javaString(env, it == sessions.end() ? "" : it->second.composing);
}
JNIEXPORT jobjectArray JNICALL Java_com_aegisinput_engine_RimeBridge_nativeGetCandidates(JNIEnv* env, jobject, jlong id) {
    std::lock_guard<std::mutex> lock(engineMutex);
    auto it = sessions.find(id);
    const size_t size = it == sessions.end() ? 0 : it->second.candidates.size();
    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray result = env->NewObjectArray(static_cast<jsize>(size), stringClass, nullptr);
    for (size_t i = 0; i < size; ++i) {
        jstring text = javaString(env, it->second.candidates[i]);
        env->SetObjectArrayElement(result, static_cast<jsize>(i), text);
        env->DeleteLocalRef(text);
    }
    return result;
}
JNIEXPORT jstring JNICALL Java_com_aegisinput_engine_RimeBridge_nativeCommitComposition(JNIEnv* env, jobject, jlong id) {
    std::lock_guard<std::mutex> lock(engineMutex);
    auto it = sessions.find(id);
    return javaString(env, it == sessions.end() ? "" : it->second.commit());
}
JNIEXPORT jstring JNICALL Java_com_aegisinput_engine_RimeBridge_nativeSelectCandidate(JNIEnv* env, jobject, jlong id, jint index) {
    std::lock_guard<std::mutex> lock(engineMutex);
    auto it = sessions.find(id);
    return javaString(env, it == sessions.end() ? "" : it->second.select(index));
}
JNIEXPORT void JNICALL Java_com_aegisinput_engine_RimeBridge_nativeClearComposition(JNIEnv*, jobject, jlong id) {
    std::lock_guard<std::mutex> lock(engineMutex);
    auto it = sessions.find(id);
    if (it != sessions.end()) it->second.reset();
}
}
