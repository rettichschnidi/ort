# Notes while implementing Conan 2 support

## Changes Conan 1.x -> 2.x

| Functionality               | conan  ... for 1.x                                                    | conan ... for 2.x                                                                     |
|-----------------------------|-----------------------------------------------------------------------|---------------------------------------------------------------------------------------|
| List dependencytree         | conan info conanfile.txt -l lockfile.lock --json <out.json>           | conan graph info conanfile.py --format json > <out.json>                              |
|                             | conan info conanfile.txt --json out.json -s <dummy compiler settings> | conan graph info conanfile.py --format json -s <dummy compiler settings> > <out.json> |
| Install user configuration  | conan config install my-config.zip                                    | same?!                                                                                |
| List installed remotes      | conan remote list --raw                                               | conan remote list --format json                                                       |
| Authenticate towards remote | conan user -r <remote name> -p <password> <user name>                 | conan remote login -p  <password> <remote name> <user name>                           |
| Dump internals              | conan inspect conanfile.py --json conan-inspect.json                  | conan inspect conanfile.py --format json > conan-inspect.json                         |
| Contains data folder        | ~/.conan/data                                                         | ~/.conan2/p                                                                           |
| Contains Files              | ~/.conan/data/<packagename>/<packageversion>/_/_/export/conandata.yml | conan cache path <packagename>/<packageversion>                                       |

## Supported configuration options

- locfileName?

## TODO

- [ ] Compare old vs new outputs