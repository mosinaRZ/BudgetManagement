package ir.hamedan.budgetmanagement.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import ir.hamedan.budgetmanagement.data.local.dao.TransactionDao
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import ir.hamedan.budgetmanagement.data.security.DatabaseKeyProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class EncryptedAppDatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var transactionDao: TransactionDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val passphrase = DatabaseKeyProvider.getPassphrase(context)
        val factory = SupportOpenHelperFactory(passphrase)

        db = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        )
            .openHelperFactory(factory)
            .allowMainThreadQueries()
            .build()

        transactionDao = db.transactionDao()
        AppDatabase.clearInstance()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
        AppDatabase.clearInstance()
    }

    @Test
    fun insertAndReadTransaction_worksWithEncryption() = runTest {
        val transaction = TransactionEntity(
            id = "enc-test-1",
            title = "تست رمزنگاری",
            amount = 150_000.0,
            category = "FOOD",
            type = "EXPENSE",
            note = "رمزنگاری شده"
        )

        transactionDao.insertTransaction(transaction)

        val all = transactionDao.getAllTransactions().first()

        assertThat(all).hasSize(1)
        assertThat(all[0].id).isEqualTo("enc-test-1")
        assertThat(all[0].title).isEqualTo("تست رمزنگاری")
        assertThat(all[0].amount).isEqualTo(150_000.0)
        assertThat(all[0].note).isEqualTo("رمزنگاری شده")
    }

    @Test
    fun multipleInsertsAndQuery_workCorrectly() = runTest {
        val tx1 = TransactionEntity(
            id = "1",
            title = "درآمد",
            amount = 5_000_000.0,
            type = "INCOME",
            category = "SALARY"
        )
        val tx2 = TransactionEntity(
            id = "2",
            title = "هزینه",
            amount = 250_000.0,
            type = "EXPENSE",
            category = "FOOD"
        )

        transactionDao.insertTransaction(tx1)
        transactionDao.insertTransaction(tx2)

        val all = transactionDao.getAllTransactions().first()
        assertThat(all).hasSize(2)
    }

    @Test
    fun deleteWorksWithEncryption() = runTest {
        val tx = TransactionEntity(
            id = "to-delete",
            title = "حذف شود",
            amount = 100.0,
            type = "EXPENSE"
        )
        transactionDao.insertTransaction(tx)
        transactionDao.deleteTransactionById("to-delete")

        val all = transactionDao.getAllTransactions().first()
        assertThat(all).isEmpty()
    }
}