#include "rime_jni.h"
#include <rime_api.h>
#include <codecvt>
#include <locale>
#include <mutex>
#include <string>
#include <vector>
#include <unordered_map>

namespace {
std::mutex engineMutex;
RimeApi* api = nullptr;
bool initialized = false;
std::string dataPath, userPath;
std::unordered_map<jlong, std::string> pendingCommits;
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
std::string takeCommit(jlong id) {
    RIME_STRUCT(RimeCommit, commit);
    if (!api->get_commit(id, &commit)) return "";
    std::string text = commit.text ? commit.text : "";
    api->free_commit(&commit);
    return text;
}
const std::unordered_map<std::string, int> zhuyinKeys = {
    {"ㄅ",'1'},{"ㄆ",'q'},{"ㄇ",'a'},{"ㄈ",'z'},
    {"ㄉ",'2'},{"ㄊ",'w'},{"ㄋ",'s'},{"ㄌ",'x'},
    {"ㄍ",'e'},{"ㄎ",'d'},{"ㄏ",'c'},{"ㄐ",'r'},{"ㄑ",'f'},{"ㄒ",'v'},
    {"ㄓ",'5'},{"ㄔ",'t'},{"ㄕ",'g'},{"ㄖ",'b'},{"ㄗ",'y'},{"ㄘ",'h'},{"ㄙ",'n'},
    {"ㄧ",'u'},{"ㄨ",'j'},{"ㄩ",'m'},{"ㄚ",'8'},{"ㄛ",'i'},{"ㄜ",'k'},{"ㄝ",','},
    {"ㄞ",'9'},{"ㄟ",'o'},{"ㄠ",'l'},{"ㄡ",'.'},{"ㄢ",'0'},{"ㄣ",'p'},
    {"ㄤ",';'},{"ㄥ",'/'},{"ㄦ",'-'},{"ˉ",' '},{"ˊ",'6'},{"ˇ",'3'},{"ˋ",'4'},{"˙",'7'}
};
}
extern "C" {
JNIEXPORT void JNICALL Java_com_aegisinput_engine_RimeBridge_nativeInitialize(
        JNIEnv* env, jobject, jstring dataDir, jstring sharedDir) {
    std::lock_guard<std::mutex> lock(engineMutex);
    if (initialized) return;
    dataPath = stringValue(env, sharedDir); userPath = stringValue(env, dataDir);
    api = rime_get_api();
    RIME_STRUCT(RimeTraits, traits);
    traits.shared_data_dir = dataPath.c_str(); traits.user_data_dir = userPath.c_str();
    traits.distribution_name = "AegisInput"; traits.distribution_code_name = "aegis";
    traits.distribution_version = "1.0"; traits.app_name = "rime.aegisinput";
    traits.min_log_level = 2; traits.log_dir = userPath.c_str();
    api->setup(&traits); api->initialize(&traits);
    if (api->start_maintenance(false)) api->join_maintenance_thread();
    initialized = true;
}
JNIEXPORT void JNICALL Java_com_aegisinput_engine_RimeBridge_nativeShutdown(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lock(engineMutex);
    if (initialized) api->finalize();
    initialized = false; pendingCommits.clear();
}
JNIEXPORT jlong JNICALL Java_com_aegisinput_engine_RimeBridge_nativeCreateSession(JNIEnv* env, jobject) {
    std::lock_guard<std::mutex> lock(engineMutex);
    if (!initialized) return 0;
    auto id = api->create_session();
    if (!id || !api->select_schema(id, "aegis_pinyin")) {
        if (id) api->destroy_session(id);
        env->ThrowNew(env->FindClass("java/lang/IllegalStateException"), "Rime Pinyin schema failed to deploy");
        return 0;
    }
    api->set_option(id, "ascii_mode", false);
    pendingCommits[id] = "";
    return id;
}
JNIEXPORT void JNICALL Java_com_aegisinput_engine_RimeBridge_nativeDestroySession(JNIEnv*, jobject, jlong id) {
    std::lock_guard<std::mutex> lock(engineMutex);
    api->destroy_session(id); pendingCommits.erase(id);
}
JNIEXPORT jboolean JNICALL Java_com_aegisinput_engine_RimeBridge_nativeSelectSchema(JNIEnv* env, jobject, jlong id, jstring schema) {
    std::lock_guard<std::mutex> lock(engineMutex);
    api->clear_composition(id); pendingCommits[id].clear();
    const bool success = api->select_schema(id, stringValue(env,schema).c_str());
    api->set_option(id, "ascii_mode", false);
    return success;
}
JNIEXPORT jboolean JNICALL Java_com_aegisinput_engine_RimeBridge_nativeProcessKey(JNIEnv* env, jobject, jlong id, jstring value) {
    std::lock_guard<std::mutex> lock(engineMutex);
    auto key = stringValue(env,value);
    int code = key == "BackSpace" ? 0xff08 : (key.size()==1 ? static_cast<unsigned char>(key[0]) : 0);
    auto zh = zhuyinKeys.find(key);
    if (zh != zhuyinKeys.end()) code = zh->second;
    if (!code) return false;
    bool handled = api->process_key(id, code, 0);
    pendingCommits[id] += takeCommit(id);
    return handled;
}
JNIEXPORT jstring JNICALL Java_com_aegisinput_engine_RimeBridge_nativeGetComposingText(JNIEnv* env, jobject, jlong id) {
    std::lock_guard<std::mutex> lock(engineMutex);
    RIME_STRUCT(RimeContext, context);
    std::string text;
    if (api->get_context(id,&context)) {
        if (context.composition.preedit) text=context.composition.preedit;
        api->free_context(&context);
    }
    return javaString(env,text);
}
JNIEXPORT jobjectArray JNICALL Java_com_aegisinput_engine_RimeBridge_nativeGetCandidates(JNIEnv* env, jobject, jlong id) {
    std::lock_guard<std::mutex> lock(engineMutex);
    std::vector<std::string> candidates;
    RimeCandidateListIterator iterator{};
    if (api->candidate_list_begin(id, &iterator)) {
        while (candidates.size()<60 && api->candidate_list_next(&iterator))
            candidates.emplace_back(iterator.candidate.text);
        api->candidate_list_end(&iterator);
    }
    auto result=env->NewObjectArray(candidates.size(),env->FindClass("java/lang/String"),nullptr);
    for(size_t i=0;i<candidates.size();++i) {
        auto text=javaString(env,candidates[i]); env->SetObjectArrayElement(result,i,text); env->DeleteLocalRef(text);
    }
    return result;
}
JNIEXPORT jstring JNICALL Java_com_aegisinput_engine_RimeBridge_nativeCommitComposition(JNIEnv* env, jobject, jlong id) {
    std::lock_guard<std::mutex> lock(engineMutex);
    auto text = pendingCommits[id]; pendingCommits[id].clear(); return javaString(env,text);
}
JNIEXPORT jstring JNICALL Java_com_aegisinput_engine_RimeBridge_nativeSelectCandidate(JNIEnv* env, jobject, jlong id, jint index) {
    std::lock_guard<std::mutex> lock(engineMutex);
    if(index<0 || !api->select_candidate(id,index)) return javaString(env, "");
    auto text=pendingCommits[id]+takeCommit(id); pendingCommits[id].clear(); return javaString(env,text);
}
JNIEXPORT void JNICALL Java_com_aegisinput_engine_RimeBridge_nativeClearComposition(JNIEnv*, jobject, jlong id) {
    std::lock_guard<std::mutex> lock(engineMutex);
    api->clear_composition(id); pendingCommits[id].clear();
}
}
