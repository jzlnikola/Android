package elfak.mosis.medspot.models.data

data class MarkerItem(var name: String? = null,
                      var longitude: String? = null,
                      var latitude: String? = null,
                      var user: String? = null,
                      var points: Int? = null,
                      var rating: Double? = null,
                      var ratingCount: Int? = null,
                      var dateCreated: String? = null,
                      var hash: String? = null,
                      var id: String? = null
)