import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.*;
import static org.junit.jupiter.api.Assertions.*;

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

class BankTest {

    double delta = 0.001; // small delta for testing
    File recordFile = new File("BankRecord.txt"); // As the file name is hardcoded
    InputStream realIn = System.in;
    PrintStream realOut = System.out;
    ByteArrayOutputStream out;
    Bank bank;

    /// ///////////////////Doing Setup/////////////////
    @BeforeEach
    void setUp() {
        bank = new Bank();
        out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
        deleteRecordFile();
    }

    @AfterEach
    void tearDown() {
        System.setIn(realIn);
        System.setOut(realOut);
        deleteRecordFile();
    }

    void feed(String s) {
        System.setIn(new ByteArrayInputStream(s.getBytes()));
    }

    String output() {
        return out.toString();
    }


    // load() function never closes its stream. Running gc on loop for that so that it deletes
    void deleteRecordFile() {
        for (int attempt = 0; attempt < 3 && recordFile.exists(); attempt++) {
            if (recordFile.delete()) {
                return;
            }
            System.gc();
        }
    }

    // helper to stop typing out the four prompts everytime.
    void addAccount(String name, String acc, String pin, String deposit) {
        feed(name + "\n" + acc + "\n" + pin + "\n" + deposit + "\n");
        bank.addNewRecord();
    }

    /////////////addNewRecord Function////////////////////

    @Test
    void addNewRecordReadsAllFourPrompts() {
        addAccount("Clint Ferreira", "12345678", "1234", "500");

        Account a = bank.AL.get(0);
        assertEquals(1, bank.AL.size());
        assertEquals("Clint Ferreira", a.getName());
        assertEquals(12345678, a.getAccountNumber());
        assertEquals("1234", a.getPIN());
        assertEquals(1500, a.getAmount(), delta);
    }

    @Test
    void addNewRecordWithZeroDeposit() { // adding empty deposit account
        addAccount("Niku", "87654321", "0000", "0");
        assertEquals(1000, bank.AL.get(0).getAmount(), delta);
    }

    @Test
    void canAddMoreThanOneAccount() {
        addAccount("Nikita", "11111111", "1111", "0");
        addAccount("Ana", "22222222", "2222", "100");

        assertEquals(2, bank.AL.size());
        assertEquals("Nikita", bank.AL.get(0).getName());
        assertEquals("Ana", bank.AL.get(1).getName());
        assertEquals(1100, bank.AL.get(1).getAmount(), delta);
    }

    // checking long name with spaces
    @Test
    void nameWithSpacesIsNotCutOff() {
        addAccount("Mary Jane Watson", "13571357", "8080", "25");
        assertEquals("Mary Jane Watson", bank.AL.get(0).getName());
    }

    //////////transfer Function//////////////

    @Test
    void transferMovesTheMoney() {
        addAccount("Sam", "11111111", "1111", "0");
        addAccount("Ana", "22222222", "2222", "0");

        feed("11111111\n1111\n22222222\n300\n");
        bank.transfer();

        assertAll(
                () -> assertEquals(700, bank.AL.get(0).getAmount(), delta),
                () -> assertEquals(1300, bank.AL.get(1).getAmount(), delta)
        );
    }

    // Checking full amount transfer
    @Test
    void canTransferTheWholeBalance() {
        addAccount("Sam", "11111111", "1111", "0");
        addAccount("Ana", "22222222", "2222", "0");

        feed("11111111\n1111\n22222222\n1000\n");
        bank.transfer();

        assertEquals(0, bank.AL.get(0).getAmount(), delta);
        assertEquals(2000, bank.AL.get(1).getAmount(), delta);
    }

    // and 1 over full amount which should be rejected
    @Test
    void transferMoreThanBalanceIsRejected() {
        addAccount("Sam", "11111111", "1111", "0");
        addAccount("Ana", "22222222", "2222", "0");

        feed("11111111\n1111\n22222222\n1001\n");
        bank.transfer();

        assertEquals(1000, bank.AL.get(0).getAmount(), delta);
        assertEquals(1000, bank.AL.get(1).getAmount(), delta);
        assertTrue(output().contains("does not have this much balance"));
    }

    @Test
    void transferWithUnknownSenderAccount() { // transfer from random account, which should be stopped
        addAccount("Sam", "11111111", "1111", "0");

        feed("99999999\n1111\n11111111\n50\n");
        bank.transfer();

        assertEquals(1000, bank.AL.get(0).getAmount(), delta);
        assertTrue(output().contains("Account not Found"));
    }

    // account number is right, pin is wrong.
    @Test
    void transferWithWrongSenderPin() {
        addAccount("Sam", "11111111", "1111", "0");
        addAccount("Ana", "22222222", "2222", "0");

        feed("11111111\n9999\n22222222\n50\n");
        bank.transfer();

        assertEquals(1000, bank.AL.get(0).getAmount(), delta);
        assertEquals(1000, bank.AL.get(1).getAmount(), delta);
        assertTrue(output().contains("Account not Found"));
    }

    @Test
    void transferWithUnknownReceiver() {    // transfer to random account, which should be stopped
        addAccount("Sam", "11111111", "1111", "0");
        addAccount("Ana", "22222222", "2222", "0");

        feed("11111111\n1111\n88888888\n50\n");
        bank.transfer();

        assertEquals(1000, bank.AL.get(0).getAmount(), delta);
        assertTrue(output().contains("Receiver's account not Found"));
    }

    // Self transfer. Nothing should change
    @Test
    void transferToYourselfChangesNothing() {
        addAccount("Sam", "11111111", "1111", "0");

        feed("11111111\n1111\n11111111\n400\n");
        bank.transfer();

        assertEquals(1000, bank.AL.get(0).getAmount(), delta);
    }

    @Test ///  transfer when no accounts created
    void transferWhenThereAreNoAccountsAtAll() {
        feed("11111111\n1111\n22222222\n50\n");
        bank.transfer();

        assertTrue(bank.AL.isEmpty());
        assertTrue(output().contains("Account not Found"));
    }

    /////////////////////////////  withdraw function  //////////////////////////

    @Test // Basic Check
    void withdrawTakesTheMoneyOut() {
        addAccount("Sam", "11111111", "1111", "500");

        feed("11111111\n1111\n200\n");
        bank.withdraw();

        assertEquals(1300, bank.AL.get(0).getAmount(), delta);
    }

    @Test // Full Balance Withdraw
    void canWithdrawEverything() {
        addAccount("Sam", "11111111", "1111", "0");

        feed("11111111\n1111\n1000\n");
        bank.withdraw();

        assertEquals(0, bank.AL.get(0).getAmount(), delta);
    }

    @Test // Trying to over withdraw
    void withdrawMoreThanBalanceIsRejected() {
        addAccount("Sam", "11111111", "1111", "0");

        feed("11111111\n1111\n1001\n");
        bank.withdraw();

        assertEquals(1000, bank.AL.get(0).getAmount(), delta);
        assertTrue(output().contains("does not have this much balance"));
    }

    @Test // Witthdraw with invalid account No
    void withdrawWithUnknownAccount() {
        addAccount("Sam", "11111111", "1111", "0");

        feed("99999999\n1111\n50\n");
        bank.withdraw();

        assertEquals(1000, bank.AL.get(0).getAmount(), delta);
        assertTrue(output().contains("Account not Found"));
    }

    @Test // check wrong pin
    void withdrawWithWrongPin() {
        addAccount("Sam", "11111111", "1111", "0");
        addAccount("Ana", "22222222", "2222", "0");

        feed("22222222\n0000\n50\n");
        bank.withdraw();

        assertEquals(1000, bank.AL.get(1).getAmount(), delta);
        assertTrue(output().contains("Account not Found"));
    }

    @Test // Withdraw without account
    void withdrawWhenBankIsEmpty() {
        feed("11111111\n1111\n50\n");
        bank.withdraw();
        assertTrue(output().contains("Account not Found"));
    }

    /////////////////// print Function ///////////////////////////

    @Test
    void printShowsEveryAccount() { // Print all
        addAccount("Sam", "11111111", "1111", "0");
        addAccount("Ana", "22222222", "2222", "250");

        bank.print();
        String text = output();

        assertTrue(text.contains("Sam"));
        assertTrue(text.contains("11111111"));
        assertTrue(text.contains("1000.0"));
        assertTrue(text.contains("Ana"));
        assertTrue(text.contains("22222222"));
        assertTrue(text.contains("1250.0"));
    }

    @Test // Print Without accounts
    void printWithNoAccounts() {
        bank.print();
        assertFalse(output().contains("Account Number:"));
    }

    ////////// save / load function ////////////////

    @Test   // Check accounts after save and load
    void saveThenLoadGivesBackTheSameAccounts() {
        addAccount("Sam", "11111111", "1111", "0");
        addAccount("Ana", "22222222", "2222", "500");
        bank.save();

        Bank b2 = new Bank();
        b2.load();

        assertTrue(recordFile.exists());
        assertEquals(2, b2.AL.size());
        assertEquals("Sam", b2.AL.get(0).getName());
        assertEquals(11111111, b2.AL.get(0).getAccountNumber());
        assertEquals("1111", b2.AL.get(0).getPIN());
        assertEquals(1000, b2.AL.get(0).getAmount(), delta);
        assertEquals("Ana", b2.AL.get(1).getName());
        assertEquals(1500, b2.AL.get(1).getAmount(), delta);
    }

    @Test // Try Save an empty bank
    void savingAnEmptyBankStillMakesAFile() {
        bank.save();

        Bank b2 = new Bank();
        b2.load();

        assertTrue(recordFile.exists());
        assertTrue(b2.AL.isEmpty());
        assertFalse(output().contains("Error Saving Data"));
    }

    // load  function has an empty catch block so a missing file just gives an empty bank
    @Test
    void loadWithNoFileThere() {
        assertFalse(recordFile.exists());

        bank.load();

        assertTrue(bank.AL.isEmpty());
        assertEquals("", output());
    }

    // load stops after null is found
    @Test
    void loadStopsWhenItHitsANull() throws Exception {
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(recordFile));
        oos.writeObject(new Account("Sam", 11111111, "1111", 0));
        oos.writeObject(null);
        oos.writeObject(new Account("Ghost", 99999999, "9999", 0));
        oos.close();
        bank.load();

        assertEquals(1, bank.AL.size());
        assertEquals("Sam", bank.AL.get(0).getName());
        assertEquals(1000, bank.AL.get(0).getAmount(), delta);
    }

    @Test // check load on non serialised data
    void loadDoesNotCrashOnAJunkFile() throws Exception {
        FileOutputStream fos = new FileOutputStream(recordFile);
        fos.write("this is not serialised data. Just junk file".getBytes());
        fos.close();

        bank.load();
        assertTrue(bank.AL.isEmpty());
    }

    // load adds to the list instead of clearing it first, so calling it twice duplicates everything.
    @Test
    void loadAppendsAndDoesNotReplace() {
        addAccount("Sam", "11111111", "1111", "0");
        bank.save();

        bank.load();

        assertEquals(2, bank.AL.size());
        assertEquals("Sam", bank.AL.get(1).getName());
    }


    @Test
    void saveReportsAnErrorWhenItCannotWriteTheFile() {
        addAccount("Sam", "11111111", "1111", "0");
        deleteRecordFile();

        assertTrue(recordFile.mkdir(), "setup: could not create blocking directory");

        bank.save();

        assertTrue(output().contains("Error Saving Data to File"));

        recordFile.delete();
    }
}