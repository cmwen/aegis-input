#include <rime_api.h>
#include <iostream>
#include <stdexcept>
#include <string>

void require(bool value, const std::string& message) {
    if (!value) throw std::runtime_error(message);
}
void convert(RimeApi* api, const char* schema, const char* keys, const char* expected) {
    const auto session = api->create_session();
    require(session && api->select_schema(session, schema), std::string("Cannot load ") + schema);
    api->set_option(session, "ascii_mode", false);
    for (const char* key = keys; *key; ++key) api->process_key(session, *key, 0);
    RimeCandidateListIterator candidates{};
    int index = -1, position = 0;
    if (api->candidate_list_begin(session, &candidates)) {
        while (position < 60 && api->candidate_list_next(&candidates)) {
            if (std::string(candidates.candidate.text) == expected) { index = position; break; }
            ++position;
        }
        api->candidate_list_end(&candidates);
    }
    require(index >= 0, std::string("Missing ") + expected + " for " + keys);
    require(api->select_candidate(session, index), "Candidate selection failed");
    RIME_STRUCT(RimeCommit, commit);
    require(api->get_commit(session, &commit), "Selection did not commit");
    require(std::string(commit.text) == expected, "Wrong commit text");
    api->free_commit(&commit);
    api->process_key(session, schema == std::string("aegis_zhuyin") ? 's' : 'n', 0);
    api->clear_composition(session);
    RIME_STRUCT(RimeContext, context);
    if (api->get_context(session, &context)) {
        require(context.composition.length == 0, "Reset retained composition");
        api->free_context(&context);
    }
    api->destroy_session(session);
    std::cout << schema << ": " << keys << " -> " << expected << std::endl;
}
int main(int argc, char** argv) {
    if (argc != 3) return 2;
    auto* api = rime_get_api();
    RIME_STRUCT(RimeTraits, traits);
    traits.shared_data_dir = argv[1]; traits.user_data_dir = argv[2];
    traits.app_name = "rime.aegis-test"; traits.min_log_level = 2; traits.log_dir = argv[2];
    api->setup(&traits); api->initialize(&traits);
    if (api->start_maintenance(true)) api->join_maintenance_thread();
    try {
        convert(api, "aegis_pinyin", "nihao", "你好");
        convert(api, "aegis_pinyin", "zongwen", "中文");
        convert(api, "aegis_zhuyin", "su3cl3", "你好");
        convert(api, "aegis_zhuyin", "yj/ jp6", "中文");
    } catch (const std::exception& error) {
        std::cerr << error.what() << std::endl; api->finalize(); return 1;
    }
    api->finalize();
}
