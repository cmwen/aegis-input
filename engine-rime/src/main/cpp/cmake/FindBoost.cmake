set(Boost_FOUND TRUE)
file(GLOB Boost_INCLUDE_DIRS LIST_DIRECTORIES TRUE "${AEGIS_VENDOR}/boost/libs/*/include")
set(Boost_LIBRARIES Boost::regex)
