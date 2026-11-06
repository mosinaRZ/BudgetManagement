package appversion

import "testing"

func TestParse(t *testing.T) {
	good := map[string]Version{
		"1.2.0":        {1, 2, 0},
		"1.2":          {1, 2, 0},
		"3":            {3, 0, 0},
		" v1.10.4 ":    {1, 10, 4},
		"1.2.0-debug":  {1, 2, 0},
		"1.1.2+build7": {1, 1, 2},
	}
	for in, want := range good {
		got, ok := Parse(in)
		if !ok || got != want {
			t.Errorf("Parse(%q) = %v, %v; want %v, true", in, got, ok, want)
		}
	}
	for _, in := range []string{"", "   ", "abc", "-1.2.0", "1..2", "1.2.3.4", "1.2.", "1234567.0.0", ".5"} {
		if v, ok := Parse(in); ok {
			t.Errorf("Parse(%q) = %v, true; want false", in, v)
		}
	}
}

func TestCompare(t *testing.T) {
	cases := []struct {
		a, b string
		want int
	}{
		{"1.1.2", "1.2.0", -1},
		{"1.2.0", "1.2.0", 0},
		{"1.2", "1.2.0", 0},
		{"1.10.0", "1.9.9", 1},
		{"2.0.0", "1.99.99", 1},
		{"1.2.1", "1.2.0", 1},
	}
	for _, c := range cases {
		a, _ := Parse(c.a)
		b, _ := Parse(c.b)
		if got := a.Compare(b); got != c.want {
			t.Errorf("Compare(%s, %s) = %d; want %d", c.a, c.b, got, c.want)
		}
	}
	lo, _ := Parse("1.1.2")
	hi, _ := Parse("1.2.0")
	if !lo.Less(hi) || hi.Less(lo) || lo.Less(lo) {
		t.Error("Less is inconsistent with Compare")
	}
}
