_declare_dirvar() { local _dir="${BASH_SOURCE[0]}"; _dir="${_dir%/*}"; local varname="${1:-dir}"; local count="${2:-1}"; while [[ "$count" -gt 0 ]]; do _dir="${_dir%/*}"; let count-- || :; done; eval "$varname"'="$_dir"'; }
_declare_dirvar _projdefdir 0
_declare_dirvar _projbasedir 1

_declare_dirvar project__projectionist__Droot 1
export project__projectionist__Dprojroot="$project__projectionist__Droot/.tsproject"
export project__projectionist__Droot

projectionist_ide() {
    "$project__currentproject__Droot/bin/markwin_vimide"
    "$project__currentproject__Droot/bin/mark_last_servername" projectionist
    "$project__projectionist__Dprojroot/ide" --projname=projectionist "$@"
}
