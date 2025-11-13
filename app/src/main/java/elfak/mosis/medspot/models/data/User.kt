package elfak.mosis.medspot.models.data

data class User(var username: String? = null,
                var password: String? = null,
                var firstname: String? = null,
                var lastname: String? = null,
                var phone: String? = null,
                var imageBase64: String? = null,
                var points: Int? = null,
) {
}
