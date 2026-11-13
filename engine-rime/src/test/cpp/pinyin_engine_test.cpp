#include "pinyin_engine.h"
#include <cassert>
#include <chrono>
#include <iostream>

int main(int argc, char** argv) {
    assert(argc == 2);
    aegis::PinyinEngine engine;
    const auto start = std::chrono::steady_clock::now();
    engine.load(argv[1]);
    aegis::PinyinSession session;
    auto type = [&](const std::string& text) {
        session.reset();
        for (char c : text) assert(session.process(std::string(1, c), engine));
    };
    type("nihao");
    assert(session.candidates.front() == "你好");
    assert(session.select(0) == "你好");
    assert(session.composing.empty() && session.candidates.empty());
    type("zhongwen");
    assert(session.candidates.front() == "中文");
    type("beijing");
    assert(session.candidates.front() == "北京");
    type("jisuanji");
    assert(session.candidates.front() == "计算机");
    type("ni'hao");
    assert(session.candidates.front() == "你好");
    type("xi'an");
    assert(std::find(session.candidates.begin(), session.candidates.end(), "西安") != session.candidates.end());
    assert(std::find(session.candidates.begin(), session.candidates.end(), "先") == session.candidates.end());
    type("nihaozhongwen"); // A phrase absent from the source lexicon: compose multiple words.
    assert(session.candidates.front() == "你好中文");
    assert(session.process("BackSpace", engine));
    assert(session.composing == "nihaozhongwe");
    assert(session.process("n", engine));
    assert(session.select(0) == "你好中文");
    type("n");
    assert(!session.candidates.empty());
    assert(!session.process("%", engine));
    assert(session.composing == "n");
    assert(session.select(-1).empty() && session.composing == "n");
    assert(session.select(100).empty() && session.composing == "n");
    assert(session.commit() == "n");
    assert(!session.process("BackSpace", engine));
    type(std::string(64, 'a'));
    assert(!session.process("a", engine));
    assert(session.composing.size() == 64);
    aegis::PinyinSession second;
    assert(second.composing.empty());
    bool missingRejected = false;
    try { aegis::PinyinEngine missing; missing.load("/nonexistent-pinyin-dictionary"); }
    catch (const std::runtime_error&) { missingRejected = true; }
    assert(missingRejected);
    std::cout << "Offline native Pinyin tests passed (load + typing "
              << std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::steady_clock::now() - start).count()
              << " ms)\n";
}
