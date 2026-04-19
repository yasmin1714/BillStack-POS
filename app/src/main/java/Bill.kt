data class Bill(
    val id: Int = 0,
    val items: String,     // CSV format: "Milk,Bread,Eggs"
    val prices: String,    // CSV format: "2.50,1.20,4.00"
    val billTotal: String,
    val date: String = ""  // Handled by SQLite autogenerate
)