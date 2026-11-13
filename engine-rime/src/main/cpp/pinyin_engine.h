#pragma once

#include <algorithm>
#include <cmath>
#include <fstream>
#include <map>
#include <set>
#include <sstream>
#include <stdexcept>
#include <string>
#include <vector>

namespace aegis {
// Independent, dictionary-backed full-Pinyin decoder. No network or user-text logging.
class PinyinEngine {
    struct Entry {
        std::string text;
        std::vector<size_t> boundaries;
        double weight;
    };
    struct Path { std::string text; double score; };
    std::map<std::string, std::vector<Entry>> dictionary;
    static constexpr size_t beamSize = 12;
    static void prune(std::vector<Path>& paths) {
        std::sort(paths.begin(), paths.end(), [](const Path& a, const Path& b) {
            return a.score == b.score ? a.text < b.text : a.score > b.score;
        });
        std::set<std::string> seen;
        paths.erase(std::remove_if(paths.begin(), paths.end(), [&](const Path& path) {
            return !seen.insert(path.text).second;
        }), paths.end());
        if (paths.size() > beamSize) paths.resize(beamSize);
    }
    static bool boundariesMatch(const Entry& entry, size_t start, size_t end,
                                const std::set<size_t>& required) {
        for (size_t boundary : required) {
            if (boundary > start && boundary < end &&
                std::find(entry.boundaries.begin(), entry.boundaries.end(), boundary - start) ==
                entry.boundaries.end()) return false;
        }
        return true;
    }
public:
    void load(const std::string& filename) {
        std::ifstream input(filename);
        if (!input) throw std::runtime_error("Offline Pinyin dictionary is missing");
        dictionary.clear();
        std::string line;
        bool body = false;
        size_t entries = 0;
        while (std::getline(input, line)) {
            if (!body) { if (line == "...") body = true; continue; }
            if (line.empty() || line.front() == '#') continue;
            const auto first = line.find('\t');
            const auto second = line.find('\t', first == std::string::npos ? 0 : first + 1);
            if (first == std::string::npos || second == std::string::npos) continue;
            Entry entry{line.substr(0, first), {}, 0};
            std::string code;
            bool valid = true;
            for (char c : line.substr(first + 1, second - first - 1)) {
                if (c == ' ') entry.boundaries.push_back(code.size());
                else if (c >= 'a' && c <= 'z') code += c;
                else { valid = false; break; }
            }
            if (!valid || code.empty()) continue;
            try { entry.weight = std::max(0.0, std::stod(line.substr(second + 1))); }
            catch (...) { continue; }
            dictionary[code].push_back(std::move(entry));
            ++entries;
        }
        if (entries < 50000) { dictionary.clear(); throw std::runtime_error("Offline Pinyin dictionary is incomplete"); }
        for (auto& item : dictionary) {
            std::stable_sort(item.second.begin(), item.second.end(), [](const Entry& a, const Entry& b) {
                return a.weight > b.weight;
            });
        }
    }
    std::vector<std::string> candidates(const std::string& composing) const {
        std::string code;
        std::set<size_t> boundaries;
        for (char c : composing) {
            if (c == '\'') boundaries.insert(code.size()); else code += c;
        }
        if (code.empty() || code.size() > 64) return {};
        std::vector<Path> direct;
        const auto exact = dictionary.find(code);
        if (exact != dictionary.end()) for (const auto& entry : exact->second) {
            if (boundariesMatch(entry, 0, code.size(), boundaries))
                direct.push_back({entry.text, std::log1p(entry.weight)});
        }
        prune(direct);
        // Each cell is a bounded beam of complete dictionary-word segmentations.
        std::vector<std::vector<Path>> paths(code.size() + 1);
        paths[0].push_back({"", 0});
        for (size_t start = 0; start < code.size(); ++start) {
            if (paths[start].empty()) continue;
            prune(paths[start]);
            for (size_t end = start + 1; end <= code.size(); ++end) {
                auto found = dictionary.find(code.substr(start, end - start));
                if (found == dictionary.end()) continue;
                size_t count = 0;
                for (const auto& entry : found->second) {
                    if (!boundariesMatch(entry, start, end, boundaries)) continue;
                    // Log unigram probability penalizes unlikely words and extra splits.
                    for (const auto& path : paths[start])
                        paths[end].push_back({path.text + entry.text,
                            path.score + std::log1p(entry.weight) - 18.0});
                    if (++count == beamSize) break;
                }
                prune(paths[end]);
            }
        }
        // Complete only the final word; all earlier segments must already be valid.
        std::vector<Path> completions;
        for (size_t start = 0; start < code.size(); ++start) {
            if (paths[start].empty()) continue;
            const std::string prefix = code.substr(start);
            for (auto it = dictionary.lower_bound(prefix); it != dictionary.end() &&
                    it->first.compare(0, prefix.size(), prefix) == 0; ++it) {
                if (it->first == prefix) continue;
                size_t count = 0;
                for (const auto& entry : it->second) {
                    if (!boundariesMatch(entry, start, code.size(), boundaries)) continue;
                    for (const auto& path : paths[start]) completions.push_back({path.text + entry.text,
                        path.score + std::log1p(entry.weight) - 18.0 -
                        2.0 * static_cast<double>(it->first.size() - prefix.size())});
                    if (++count == 3) break;
                }
                if (completions.size() > 1000) prune(completions);
            }
        }
        prune(completions);
        std::vector<std::string> result;
        std::set<std::string> seen;
        for (const auto* group : {&direct, &paths.back(), &completions}) {
            for (const auto& path : *group) {
                if (seen.insert(path.text).second) result.push_back(path.text);
                if (result.size() == beamSize) return result;
            }
        }
        return result;
    }
};

struct PinyinSession {
    std::string composing;
    std::vector<std::string> candidates;
    bool process(const std::string& key, const PinyinEngine& engine) {
        if (key == "BackSpace") {
            if (composing.empty()) return false;
            composing.pop_back();
        } else if (key.size() == 1 && key[0] >= 'a' && key[0] <= 'z') {
            if (composing.size() >= 64) return false;
            composing += key;
        } else if (key == "'" && !composing.empty() && composing.back() != '\'' && composing.size() < 64) {
            composing += key;
        } else return false;
        candidates = engine.candidates(composing);
        return true;
    }
    void reset() { composing.clear(); candidates.clear(); }
    std::string commit() { auto text = composing; reset(); return text; }
    std::string select(int index) {
        if (index < 0 || static_cast<size_t>(index) >= candidates.size()) return "";
        auto text = candidates[index]; reset(); return text;
    }
};
} // namespace aegis
