import org.junit.jupiter.api.Test;
import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

// Tests for the Account class.
// Account mostly only has getters and setters
// but the 4 arg constructor does need a few test cases.
class AccountTest {

    double delta = 0.001; // Small delta number for testing

    @Test
    void defaultConstructorGivesEmptyAccount() {
        Account a = new Account();
        assertNull(a.getName());
        assertEquals(0, a.getAccountNumber());
        assertNull(a.getPIN());
        assertEquals(0, a.getAmount(), delta);
    }

    @Test
    void constructorAddsDefaultThousand() {     //Testing default add 1000
        Account a = new Account("Sam", 12345678, "1234", 500);
        assertEquals(1500, a.getAmount(), delta);
    }


    @Test
    void constructorSetsTheOtherFieldsToo() {   // Testing other Fields
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
    void zeroDepositStillGets1000() {
        Account a = new Account("Ana", 87654321, "0000", 0);
        assertEquals(1000, a.getAmount(), delta);
    }

    // A negative number deposit
    @Test
    void negativeDepositIsAllowed() {
        Account a = new Account("Ravi", 11112222, "9999", -250);
        assertEquals(750, a.getAmount(), delta);
    }

    // boundary condition + 1000 -1000
    @Test
    void depositOfMinus1000LeavesZero() {
        Account a = new Account("Mia", 33334444, "4321", -1000);
        assertEquals(0, a.getAmount(), delta);
    }

    @Test // checking for empty name and pin
    void nullNameAndPinAreAccepted() {
        Account a = new Account(null, 0, null, 100);

        assertNull(a.getName());
        assertNull(a.getPIN());
        assertEquals(1100, a.getAmount(), delta);
    }

    @Test // Checking  Setters
    void settersWork() {
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
    void settersOverwriteWhatTheConstructorSet() {
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
    void setAmountLetsYouGoNegative() {
        Account a = new Account();
        a.setAmount(-50);
        assertEquals(-50, a.getAmount(), delta);
    }

    @Test // checking if decimals work on amount
    void amountKeepsDecimals() {
        Account a = new Account();
        a.setAmount(1234.56);
        assertEquals(1234.56, a.getAmount(), delta);
    }



    // As bank.save() is writing all objects as a file, checking if serialisable
    @Test
    void accountCanBeSerialised() throws Exception {
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