#include "rime_jni.h"
#include <android/log.h>
#include <string>
#include <vector>
#include <map>

#define LOG_TAG "AegisInput-JNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// TODO: Replace with actual librime headers when available
// #include <rime_api.h>

namespace {
    // Stub session state for development without librime
    struct StubSession {
        std::string composing;
        std::vector<std::string> candidates;
    };

    std::map<jlong, StubSession> sessions;
    jlong nextSessionId = 1;
    bool initialized = false;

    std::vector<std::string> getStubCandidates(const std::string &composing) {
        std::vector<std::string> candidates;
        
        static std::map<std::string, std::vector<std::string>> dict;
        static bool populated = false;
        if (!populated) {
            dict["1"] = {"不", "爸", "包", "半", "本", "比", "邊", "並"};
            dict["q"] = {"跑", "朋", "平", "旁", "配", "破", "票", "婆"};
            dict["a"] = {"沒", "們", "買", "賣", "米", "馬", "慢", "名"};
            dict["z"] = {"發", "分", "法", "放", "風", "反", "非", "否"};
            dict["2"] = {"的", "得", "到", "大", "多", "地", "代", "當"};
            dict["w"] = {"他", "她", "它", "聽", "天", "同", "條", "體"};
            dict["s"] = {"你", "我", "那", "年", "內", "能", "南", "難"};
            dict["x"] = {"了", "來", "里", "路", "老", "六", "兩", "立"};
            dict["e"] = {"個", "國", "過", "給", "公", "高", "工", "關"};
            dict["d"] = {"看", "開", "可", "快", "口", "科", "考", "哭"};
            dict["c"] = {"好", "後", "和", "會", "話", "很", "花", "活"};
            dict["r"] = {"家", "進", "經", "幾", "機", "結", "件", "建"};
            dict["f"] = {"去", "起", "前", "期", "氣", "錢", "強", "請"};
            dict["v"] = {"寫", "新", "想", "小", "些", "先", "現", "學"};
            dict["5"] = {"這", "只", "中", "真", "正", "在", "子", "自"};
            dict["t"] = {"出", "長", "成", "車", "此", "才", "從", "次"};
            dict["g"] = {"是", "說", "書", "時", "三", "四", "死", "所"};
            dict["b"] = {"人", "日", "入", "熱", "認", "容", "如", "若"};
            dict["u"] = {"一", "以", "意", "已", "因", "影", "衣", "引"};
            dict["j"] = {"無", "五", "物", "誤", "屋", "舞", "武"};
            dict["m"] = {"雨", "於", "與", "魚", "余", "予", "羽", "育"};
            dict["8"] = {"啊", "阿", "八"};
            dict["i"] = {"喔", "哦"};
            dict["k"] = {"俄", "餓", "額"};
            dict[","] = {"誒"};
            dict["9"] = {"愛", "哎", "哀", "矮"};
            dict["o"] = {"誒"};
            dict["l"] = {"凹", "傲", "熬"};
            dict["."] = {"偶", "歐", "肉"};
            dict["-"] = {"兒", "二", "耳"};
            dict["0"] = {"安", "昂", "案", "暗"};
            dict["p"] = {"恩", "鞥"};

            dict["1j"] = {"不", "步", "部", "補", "布"};
            dict["1j3"] = {"補", "捕"};
            dict["1j4"] = {"不", "步", "部"};
            dict["5u"] = {"之", "指", "直", "紙", "知", "執", "職"};
            dict["5u4"] = {"製", "志", "智", "質", "致"};
            dict["ji"] = {"我", "握", "沃", "臥"};
            dict["ji3"] = {"我"};
            dict["ji4"] = {"握", "沃", "臥"};
            dict["u,"] = {"也", "夜", "野", "頁", "葉", "椰"};
            dict["u,3"] = {"也", "野"};
            dict["u,4"] = {"夜", "頁", "葉"};
            dict["5ji"] = {"桌", "著", "捉", "琢"};
            dict["5ji3"] = {"卓", "濁"};
            dict["5ji4"] = {"這"};
            dict["2u"] = {"的", "地", "第", "低", "底", "敵"};
            dict["2u4"] = {"地", "第", "帝", "遞"};
            dict["2u3"] = {"底", "抵"};
            dict["xu"] = {"了", "里", "力", "離", "利", "立", "理"};
            dict["xu3"] = {"里", "理", "禮", "鯉"};
            dict["xu4"] = {"力", "利", "立", "例", "麗"};
            dict["su"] = {"你", "泥", "逆", "年", "念"};
            dict["su3"] = {"你", "擬"};
            dict["su4"] = {"逆", "匿"};
            dict["cu"] = {"好", "和", "合", "河", "何", "黑"};
            dict["cu3"] = {"好"};
            dict["cu4"] = {"和", "合", "後"};
            dict["ru"] = {"家", "加", "夾", "佳", "駕"};
            dict["ru4"] = {"駕", "嫁"};
            dict["fu"] = {"去", "七", "期", "起", "氣", "其", "奇"};
            dict["fu3"] = {"起", "啟", "企"};
            dict["fu4"] = {"去", "氣", "器", "契"};
            dict["vu"] = {"寫", "新", "想", "小", "些", "先", "現", "學"};
            dict["vu3"] = {"寫", "想", "小"};
            dict["vu4"] = {"現", "線", "限", "線"};
            dict["ru0"] = {"建", "見", "件", "間", "簡", "減"};
            dict["ru04"] = {"建", "見", "件", "健", "薦"};
            dict["ru03"] = {"簡", "減", "檢"};
            dict["fu0"] = {"前", "千", "簽", "欠", "遣"};
            dict["fu04"] = {"欠", "歉"};
            dict["fu03"] = {"遣", "淺"};
            dict["vu0"] = {"現", "先", "線", "限", "顯", "鮮"};
            dict["vu04"] = {"現", "線", "限", "憲"};
            dict["vu03"] = {"顯", "險"};
            dict["e0"] = {"關", "管", "官", "館", "觀", "慣"};
            dict["e03"] = {"管", "館"};
            dict["e04"] = {"慣", "罐"};
            dict["d0"] = {"看", "砍", "看", "刊"};
            dict["d04"] = {"看"};
            dict["d03"] = {"砍"};
            dict["c0"] = {"黃", "荒", "皇", "晃", "謊"};
            dict["c03"] = {"晃", "謊"};
            dict["c04"] = {"晃", "況"};
            populated = true;
        }

        auto it = dict.find(composing);
        if (it != dict.end()) {
            candidates = it->second;
        } else {
            // Try prefix match for fuzzy search
            for (const auto &pair : dict) {
                if (composing.rfind(pair.first, 0) == 0) {
                    for (const auto &cand : pair.second) {
                        candidates.push_back(cand);
                    }
                }
            }
            if (candidates.empty()) {
                candidates.push_back(composing);
            }
        }
        return candidates;
    }
}

extern "C" {

JNIEXPORT void JNICALL
Java_com_aegisinput_engine_RimeBridge_nativeInitialize(JNIEnv *env, jobject thiz,
                                                       jstring data_dir, jstring shared_dir) {
    const char *dataPath = env->GetStringUTFChars(data_dir, nullptr);
    const char *sharedPath = env->GetStringUTFChars(shared_dir, nullptr);

    LOGI("Initializing RIME engine: data=%s, shared=%s", dataPath, sharedPath);

    // When librime is integrated:
    // RIME_STRUCT(RimeTraits, traits);
    // traits.shared_data_dir = sharedPath;
    // traits.user_data_dir = dataPath;
    // traits.app_name = "aegisinput";
    // RimeSetup(&traits);
    // RimeInitialize(&traits);

    initialized = true;

    env->ReleaseStringUTFChars(data_dir, dataPath);
    env->ReleaseStringUTFChars(shared_dir, sharedPath);
}

JNIEXPORT void JNICALL
Java_com_aegisinput_engine_RimeBridge_nativeShutdown(JNIEnv *env, jobject thiz) {
    LOGI("Shutting down RIME engine");
    sessions.clear();
    initialized = false;
    // RimeFinalize();
}

JNIEXPORT jlong JNICALL
Java_com_aegisinput_engine_RimeBridge_nativeCreateSession(JNIEnv *env, jobject thiz) {
    jlong id = nextSessionId++;
    sessions[id] = StubSession{};
    LOGI("Created session %lld", (long long)id);
    return id;
    // return (jlong) RimeCreateSession();
}

JNIEXPORT void JNICALL
Java_com_aegisinput_engine_RimeBridge_nativeDestroySession(JNIEnv *env, jobject thiz,
                                                           jlong session_id) {
    sessions.erase(session_id);
    LOGI("Destroyed session %lld", (long long)session_id);
    // RimeDestroySession((RimeSessionId) session_id);
}

JNIEXPORT jboolean JNICALL
Java_com_aegisinput_engine_RimeBridge_nativeProcessKey(JNIEnv *env, jobject thiz,
                                                       jlong session_id, jstring key) {
    auto it = sessions.find(session_id);
    if (it == sessions.end()) return JNI_FALSE;

    const char *keyStr = env->GetStringUTFChars(key, nullptr);
    std::string k(keyStr);
    env->ReleaseStringUTFChars(key, keyStr);

    // Stub: append key to composing text
    if (k == "BackSpace") {
        if (!it->second.composing.empty()) {
            it->second.composing.pop_back();
        }
    } else {
        it->second.composing += k;
    }

    // Stub candidates
    if (it->second.composing.empty()) {
        it->second.candidates.clear();
    } else {
        it->second.candidates = getStubCandidates(it->second.composing);
    }

    return JNI_TRUE;
}

JNIEXPORT jstring JNICALL
Java_com_aegisinput_engine_RimeBridge_nativeGetComposingText(JNIEnv *env, jobject thiz,
                                                              jlong session_id) {
    auto it = sessions.find(session_id);
    if (it == sessions.end()) return env->NewStringUTF("");
    return env->NewStringUTF(it->second.composing.c_str());
}

JNIEXPORT jobjectArray JNICALL
Java_com_aegisinput_engine_RimeBridge_nativeGetCandidates(JNIEnv *env, jobject thiz,
                                                          jlong session_id) {
    auto it = sessions.find(session_id);
    jclass stringClass = env->FindClass("java/lang/String");

    if (it == sessions.end() || it->second.candidates.empty()) {
        return env->NewObjectArray(0, stringClass, nullptr);
    }

    auto &candidates = it->second.candidates;
    jobjectArray result = env->NewObjectArray(
        static_cast<jsize>(candidates.size()), stringClass, nullptr);

    for (size_t i = 0; i < candidates.size(); i++) {
        env->SetObjectArrayElement(result, static_cast<jsize>(i),
                                   env->NewStringUTF(candidates[i].c_str()));
    }

    return result;
}

JNIEXPORT jstring JNICALL
Java_com_aegisinput_engine_RimeBridge_nativeCommitComposition(JNIEnv *env, jobject thiz,
                                                              jlong session_id) {
    auto it = sessions.find(session_id);
    if (it == sessions.end()) return env->NewStringUTF("");

    std::string committed = it->second.composing;
    it->second.composing.clear();
    it->second.candidates.clear();
    return env->NewStringUTF(committed.c_str());
}

JNIEXPORT void JNICALL
Java_com_aegisinput_engine_RimeBridge_nativeClearComposition(JNIEnv *env, jobject thiz,
                                                             jlong session_id) {
    auto it = sessions.find(session_id);
    if (it != sessions.end()) {
        it->second.composing.clear();
        it->second.candidates.clear();
    }
}

} // extern "C"
