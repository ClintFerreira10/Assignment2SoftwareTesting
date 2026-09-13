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
    @SpiraTestCase(testCaseId = 50905)
    public void addNewRecordReadsAllFourPrompts() {
        addAccount("Clint Ferreira", "12345678", "1234", "500");

        Account a = bank.AL.get(0);
        assertEquals(1, bank.AL.size());
        assertEquals("Clint Ferreira", a.getName());
        assertEquals(12345678, a.getAccountNumber());
        assertEquals("1234", a.getPIN());
        assertEquals(1500, a.getAmount(), delta);
    }

    @Test
    @SpiraTestCase(testCaseId = 50907)
    public void addNewRecordWithZeroDeposit() { // adding empty deposit account
        addAccount("Niku", "87654321", "0000", "0");
        assertEquals(1000, bank.AL.get(0).getAmount(), delta);
    }

    @Test
    @SpiraTestCase(testCaseId = 50909)
    public void canAddMoreThanOneAccount() {
        addAccount("Nikita", "11111111", "1111", "0");
        addAccount("Ana", "22222222", "2222", "100");

        assertEquals(2, bank.AL.size());
        assertEquals("Nikita", bank.AL.get(0).getName());
        assertEquals("Ana", bank.AL.get(1).getName());
        assertEquals(1100, bank.AL.get(1).getAmount(), delta);
    }

    // checking long name with spaces
    @Test
    @SpiraTestCase(testCaseId = 50911)
    public void nameWithSpacesIsNotCutOff() {
        addAccount("Mary Jane Watson", "13571357", "8080", "25");
        assertEquals("Mary Jane Watson", bank.AL.get(0).getName());
    }

    //////////transfer Function//////////////

    @Test
    @SpiraTestCase(testCaseId = 50914)
    public void transferMovesTheMoney() {
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
    @SpiraTestCase(testCaseId = 50916)
    public void canTransferTheWholeBalance() {
        addAccount("Sam", "11111111", "1111", "0");
        addAccount("Ana", "22222222", "2222", "0");

        feed("11111111\n1111\n22222222\n1000\n");
        bank.transfer();

        assertEquals(0, bank.AL.get(0).getAmount(), delta);
        assertEquals(2000, bank.AL.get(1).getAmount(), delta);
    }

    // and 1 over full amount which should be rejected
    @Test
    @SpiraTestCase(testCaseId = 50921)
    public void transferMoreThanBalanceIsRejected() {
        addAccount("Sam", "11111111", "1111", "0");
        addAccount("Ana", "22222222", "2222", "0");

        feed("11111111\n1111\n22222222\n1001\n");
        bank.transfer();

        assertEquals(1000, bank.AL.get(0).getAmount(), delta);
        assertEquals(1000, bank.AL.get(1).getAmount(), delta);
        assertTrue(output().contains("does not have this much balance"));
    }

    @Test
    @SpiraTestCase(testCaseId = 50923)
    public void transferWithUnknownSenderAccount() { // transfer from random account, which should be stopped
        addAccount("Sam", "11111111", "1111", "0");

        feed("99999999\n1111\n11111111\n50\n");
        bank.transfer();

        assertEquals(1000, bank.AL.get(0).getAmount(), delta);
        assertTrue(output().contains("Account not Found"));
    }

    // account number is right, pin is wrong.
    @Test
    @SpiraTestCase(testCaseId = 50926)
    public void transferWithWrongSenderPin() {
        addAccount("Sam", "11111111", "1111", "0");
        addAccount("Ana", "22222222", "2222", "0");

        feed("11111111\n9999\n22222222\n50\n");
        bank.transfer();

        assertEquals(1000, bank.AL.get(0).getAmount(), delta);
        assertEquals(1000, bank.AL.get(1).getAmount(), delta);
        assertTrue(output().contains("Account not Found"));
    }

    @Test
    @SpiraTestCase(testCaseId = 50927)
    public void transferWithUnknownReceiver() {    // transfer to random account, which should be stopped
        addAccount("Sam", "11111111", "1111", "0");
        addAccount("Ana", "22222222", "2222", "0");

        feed("11111111\n1111\n88888888\n50\n");
        bank.transfer();

        assertEquals(1000, bank.AL.get(0).getAmount(), delta);
        assertTrue(output().contains("Receiver's account not Found"));
    }

    // Self transfer. Nothing should change
    @Test
    @SpiraTestCase(testCaseId = 50928)
    public void transferToYourselfChangesNothing() {
        addAccount("Sam", "11111111", "1111", "0");

        feed("11111111\n1111\n11111111\n400\n");
        bank.transfer();

        assertEquals(1000, bank.AL.get(0).getAmount(), delta);
    }

    @Test ///  transfer when no accounts created
    @SpiraTestCase(testCaseId = 50941)
    public void transferWhenThereAreNoAccountsAtAll() {
        feed("11111111\n1111\n22222222\n50\n");
        bank.transfer();

        assertTrue(bank.AL.isEmpty());
        assertTrue(output().contains("Account not Found"));
    }

    /////////////////////////////  withdraw function  //////////////////////////

    @Test // Basic Check
    @SpiraTestCase(testCaseId = 50954)
    public void withdrawTakesTheMoneyOut() {
        addAccount("Sam", "11111111", "1111", "500");

        feed("11111111\n1111\n200\n");
        bank.withdraw();

        assertEquals(1300, bank.AL.get(0).getAmount(), delta);
    }

    @Test // Full Balance Withdraw
    @SpiraTestCase(testCaseId = 50958)
    public void canWithdrawEverything() {
        addAccount("Sam", "11111111", "1111", "0");

        feed("11111111\n1111\n1000\n");
        bank.withdraw();

        assertEquals(0, bank.AL.get(0).getAmount(), delta);
    }

    @Test // Trying to over withdraw
    @SpiraTestCase(testCaseId = 50962)
    public void withdrawMoreThanBalanceIsRejected() {
        addAccount("Sam", "11111111", "1111", "0");

        feed("11111111\n1111\n1001\n");
        bank.withdraw();

        assertEquals(1000, bank.AL.get(0).getAmount(), delta);
        assertTrue(output().contains("does not have this much balance"));
    }

    @Test // Witthdraw with invalid account No
    @SpiraTestCase(testCaseId = 50966)
    public void withdrawWithUnknownAccount() {
        addAccount("Sam", "11111111", "1111", "0");

        feed("99999999\n1111\n50\n");
        bank.withdraw();

        assertEquals(1000, bank.AL.get(0).getAmount(), delta);
        assertTrue(output().contains("Account not Found"));
    }

    @Test // check wrong pin
    @SpiraTestCase(testCaseId = 50969)
    public void withdrawWithWrongPin() {
        addAccount("Sam", "11111111", "1111", "0");
        addAccount("Ana", "22222222", "2222", "0");

        feed("22222222\n0000\n50\n");
        bank.withdraw();

        assertEquals(1000, bank.AL.get(1).getAmount(), delta);
        assertTrue(output().contains("Account not Found"));
    }

    @Test // Withdraw without account
    @SpiraTestCase(testCaseId = 50972)
    public void withdrawWhenBankIsEmpty() {
        feed("11111111\n1111\n50\n");
        bank.withdraw();
        assertTrue(output().contains("Account not Found"));
    }

    /////////////////// print Function ///////////////////////////

    @Test
    @SpiraTestCase(testCaseId = 50975)
    public void printShowsEveryAccount() { // Print all
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
    @SpiraTestCase(testCaseId = 50978)
    public void printWithNoAccounts() {
        bank.print();
        assertFalse(output().contains("Account Number:"));
    }

    ////////// save / load function ////////////////

    @Test   // Check accounts after save and load
    @SpiraTestCase(testCaseId = 50984)
    public void saveThenLoadGivesBackTheSameAccounts() {
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
    @SpiraTestCase(testCaseId = 50985)
    public void savingAnEmptyBankStillMakesAFile() {
        bank.save();

        Bank b2 = new Bank();
        b2.load();

        assertTrue(recordFile.exists());
        assertTrue(b2.AL.isEmpty());
        assertFalse(output().contains("Error Saving Data"));
    }

    // load  function has an empty catch block so a missing file just gives an empty bank
    @Test
    @SpiraTestCase(testCaseId = 50986)
    public void loadWithNoFileThere() {
        assertFalse(recordFile.exists());

        bank.load();

        assertTrue(bank.AL.isEmpty());
        assertEquals("", output());
    }

    // load stops after null is found
    @Test
    @SpiraTestCase(testCaseId = 50987)
    public void loadStopsWhenItHitsANull() throws Exception {
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
    @SpiraTestCase(testCaseId = 50988)
    public void loadDoesNotCrashOnAJunkFile() throws Exception {
        FileOutputStream fos = new FileOutputStream(recordFile);
        fos.write("this is not serialised data. Just junk file".getBytes());
        fos.close();

        bank.load();
        assertTrue(bank.AL.isEmpty());
    }

    // load adds to the list instead of clearing it first, so calling it twice duplicates everything.
    @Test
    @SpiraTestCase(testCaseId = 50989)
    public void loadAppendsAndDoesNotReplace() {
        addAccount("Sam", "11111111", "1111", "0");
        bank.save();

        bank.load();

        assertEquals(2, bank.AL.size());
        assertEquals("Sam", bank.AL.get(1).getName());
    }


    @Test
    @SpiraTestCase(testCaseId = 50990)
    public void saveReportsAnErrorWhenItCannotWriteTheFile() {
        addAccount("Sam", "11111111", "1111", "0");
        deleteRecordFile();

        assertTrue(recordFile.mkdir(), "setup: could not create blocking directory");

        bank.save();

        assertTrue(output().contains("Error Saving Data to File"));

        recordFile.delete();
    }
}