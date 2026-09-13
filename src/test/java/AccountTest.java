import org.junit.jupiter.api.Test;
import java.io.*;
import com.inflectra.spiratest.addons.junitextension.SpiraTestCase;
import com.inflectra.spiratest.addons.junitextension.SpiraTestConfiguration;
import static org.junit.jupiter.api.Assertions.*;



@SpiraTestConfiguration (
//following are REQUIRED
        url = "https://rmit.spiraservice.net/",
        login = "s4139703",
        rssToken = "{6930BCF7-B0E9-4AF7-9C6F-CCB604BE96AA}",
        projectId = 1047
//following are OPTIONAL
        // releaseId = 7,
        // testSetId
)

// Tests for the Account class.
// Account mostly only has getters and setters
// but the 4 arg constructor does need a few test cases.
class AccountTest {

    double delta = 0.001; // Small delta number for testing

    @Test
    @SpiraTestCase(testCaseId = 50676)
    public void defaultConstructorGivesEmptyAccount() {
        Account a = new Account();
        assertNull(a.getName());
        assertEquals(0, a.getAccountNumber());
        assertNull(a.getPIN());
        assertEquals(0, a.getAmount(), delta);
    }

    @Test
    @SpiraTestCase(testCaseId = 50795)
    public void constructorAddsDefaultThousand() {     //Testing default add 1000
        Account a = new Account("Sam", 12345678, "1234", 500);
        assertEquals(1500, a.getAmount(), delta);
    }


    @Test
    @SpiraTestCase(testCaseId = 50796)
    public void constructorSetsTheOtherFieldsToo() {   // Testing other Fields
        Account a = new Account("Sam", 12345678, "1234", 500);

        assertAll(
                () -> assertEquals("Sam", a.getName()),
                () -> assertEquals(12345678, a.getAccountNumber()),
                () -> assertEquals("1234", a.getPIN()),
                () -> assertEquals(1500, a.getAmount(), delta)
        );
    }

    // the menu tells you to type 0 if you do not want to deposit any amount
    @Test
    @SpiraTestCase(testCaseId = 50797)
    public void zeroDepositStillGets1000() {
        Account a = new Account("Ana", 87654321, "0000", 0);
        assertEquals(1000, a.getAmount(), delta);
    }

    // A negative number deposit
    @Test
    @SpiraTestCase(testCaseId = 50799)
    public void negativeDepositIsAllowed() {
        Account a = new Account("Ravi", 11112222, "9999", -250);
        assertEquals(750, a.getAmount(), delta);
    }

    // boundary condition + 1000 -1000
    @Test
    @SpiraTestCase(testCaseId = 50800)
    public void depositOfMinus1000LeavesZero() {
        Account a = new Account("Mia", 33334444, "4321", -1000);
        assertEquals(0, a.getAmount(), delta);
    }

    @Test // checking for empty name and pin
    @SpiraTestCase(testCaseId = 50802)
    public void nullNameAndPinAreAccepted() {
        Account a = new Account(null, 0, null, 100);

        assertNull(a.getName());
        assertNull(a.getPIN());
        assertEquals(1100, a.getAmount(), delta);
    }

    @Test // Checking  Setters
    @SpiraTestCase(testCaseId = 50803)
    public void settersWork() {
        Account a = new Account();
        a.setName("Ana");
        a.setAccountNumber(87654321);
        a.setPIN("4321");
        a.setAmount(250);

        assertEquals("Ana", a.getName());
        assertEquals(87654321, a.getAccountNumber());
        assertEquals("4321", a.getPIN());
        assertEquals(250, a.getAmount(), delta);
    }

    @Test // checking if setters overwrite after being set by constructor
    @SpiraTestCase(testCaseId = 50804)
    public void settersOverwriteWhatTheConstructorSet() {
        Account a = new Account("Sam", 12345678, "1234", 500);
        a.setName("Samantha");
        a.setAccountNumber(99998888);
        a.setPIN("5678");
        a.setAmount(10);

        assertEquals("Samantha", a.getName());
        assertEquals(99998888, a.getAccountNumber());
        assertEquals("5678", a.getPIN());
        assertEquals(10, a.getAmount(), delta);

    }

    @Test // Check amount setter for negative
    @SpiraTestCase(testCaseId = 50805)
    public void setAmountLetsYouGoNegative() {
        Account a = new Account();
        a.setAmount(-50);
        assertEquals(-50, a.getAmount(), delta);
    }

    @Test // checking if decimals work on amount
    @SpiraTestCase(testCaseId = 50807)
    public void amountKeepsDecimals() {
        Account a = new Account();
        a.setAmount(1234.56);
        assertEquals(1234.56, a.getAmount(), delta);
    }



    // As bank.save() is writing all objects as a file, checking if serialisable
    @Test
    @SpiraTestCase(testCaseId = 50808)
    public void accountCanBeSerialised() throws Exception {
        Account original = new Account("Sam", 12345678, "1234", 500);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bytes);
        out.writeObject(original);
        out.close();

        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()));
        Account copy = (Account) in.readObject();
        in.close();

        assertTrue(original instanceof Serializable);
        assertEquals("Sam", copy.getName());
        assertEquals(12345678, copy.getAccountNumber());
        assertEquals("1234", copy.getPIN());
        assertEquals(1500, copy.getAmount(), delta);
    }
}