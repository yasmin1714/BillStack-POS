import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "Inventory.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        // Create table with ID (String, 25 chars), Name, Price, and Description
        val createTableQuery = """
            CREATE TABLE IF NOT EXISTS Product (
                id TEXT(25) PRIMARY KEY,
                name TEXT,
                price DECIMAL,
                description TEXT
            )
        """.trimIndent()
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS Product")
        onCreate(db)
    }

    // Function to insert a product
    fun addProduct(id: String, name: String, price: Double, description: String): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("id", id)
            put("name", name)
            put("price", price)
            put("description", description)
        }

        // Use insert (returns -1 if failed, e.g., if ID already exists)
        return db.insert("Product", null, values)
    }

    fun getAllProducts(): List<Product> {
        val productList = mutableListOf<Product>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM Product", null)

        if (cursor.moveToFirst()) {
            do {
                val product = Product(
                    id = cursor.getString(0),
                    name = cursor.getString(1),
                    price = cursor.getDouble(2),
                    description = cursor.getString(3)
                )
                productList.add(product)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return productList
    }

    fun getProductById(productId: String): Product? {
        val db = this.readableDatabase
        var product: Product? = null

        // Use a parameterized query for safety and speed
        val cursor = db.rawQuery("SELECT * FROM Product WHERE id = ?", arrayOf(productId))

        if (cursor.moveToFirst()) {
            product = Product(
                id = cursor.getString(0),
                name = cursor.getString(1),
                price = cursor.getDouble(2),
                description = cursor.getString(3)
            )
        }

        cursor.close()
        return product
    }
}