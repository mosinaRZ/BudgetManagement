package entity

// Gender is the optional gender preference used only for profile presentation and
// gender-specific occasion notifications. It has no authorization meaning.
type Gender string

const (
	GenderMale           Gender = "male"
	GenderFemale         Gender = "female"
	GenderPreferNotToSay Gender = "prefer_not_to_say"
)

func (g Gender) Valid() bool {
	switch g {
	case GenderMale, GenderFemale, GenderPreferNotToSay:
		return true
	default:
		return false
	}
}
