cmake_minimum_required(VERSION 3.24)

project(WinRiskV6 VERSION 0.1.0 LANGUAGES CXX)

# Keep Qt's QML code generation inside the canonical source tree on every platform.
include(CTest)
add_subdirectory("${CMAKE_CURRENT_LIST_DIR}/.." "${CMAKE_CURRENT_BINARY_DIR}/native")
