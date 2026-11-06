// Package appversion parses and compares the version of the Android app (for example "1.2.0").
package appversion

import (
	"fmt"
	"strconv"
	"strings"
)

// Version is a numeric major.minor.patch triple. Missing parts count as zero, so "1.2" == "1.2.0".
type Version struct {
	Major, Minor, Patch int
}

// Parse reads "1.2.0", "1.2" or "1", optionally prefixed with "v" and optionally followed by a
// suffix that does not start with a digit or a dot ("1.2.0-debug", "1.2.0+5"). It reports false
// when the text has no usable leading version number, so callers can decide how to treat it.
func Parse(raw string) (Version, bool) {
	s := strings.TrimSpace(raw)
	s = strings.TrimPrefix(strings.TrimPrefix(s, "v"), "V")
	end := len(s)
	for i := 0; i < len(s); i++ {
		if c := s[i]; (c < '0' || c > '9') && c != '.' {
			end = i
			break
		}
	}
	core := s[:end]
	if core == "" {
		return Version{}, false
	}
	parts := strings.Split(core, ".")
	if len(parts) > 3 {
		return Version{}, false
	}
	var nums [3]int
	for i, p := range parts {
		if p == "" || len(p) > 6 {
			return Version{}, false
		}
		n, err := strconv.Atoi(p)
		if err != nil {
			return Version{}, false
		}
		nums[i] = n
	}
	return Version{Major: nums[0], Minor: nums[1], Patch: nums[2]}, true
}

// Compare returns -1, 0 or 1 when v is lower than, equal to or higher than other.
func (v Version) Compare(other Version) int {
	switch {
	case v.Major != other.Major:
		return sign(v.Major - other.Major)
	case v.Minor != other.Minor:
		return sign(v.Minor - other.Minor)
	default:
		return sign(v.Patch - other.Patch)
	}
}

// Less reports whether v is strictly lower than other.
func (v Version) Less(other Version) bool { return v.Compare(other) < 0 }

func (v Version) String() string { return fmt.Sprintf("%d.%d.%d", v.Major, v.Minor, v.Patch) }

func sign(n int) int {
	switch {
	case n < 0:
		return -1
	case n > 0:
		return 1
	}
	return 0
}
