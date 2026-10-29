package app.views;

import app.controllers.*;
import app.models.*;
import app.utils.DataLoader;
import com.williamcallahan.tui4j.compat.bubbletea.*;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.Key;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;
import com.williamcallahan.tui4j.compat.bubbles.textinput.TextInput;
import com.williamcallahan.tui4j.compat.lipgloss.Size;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import java.io.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(15)
class TuiViewTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-28T17:00:00Z"), ZoneId.of("Asia/Manila"));
    private static final String ANSI = "\\x1B\\[[0-?]*[ -/]*[@-~]";

    private static String plain(String text) { return text.replaceAll(ANSI, ""); }
    private static KeyPressMessage key(KeyType type) { return new KeyPressMessage(new Key(type)); }
    private static KeyPressMessage text(String value) { return new KeyPressMessage(new Key(KeyType.KeyRunes, value.toCharArray())); }

    private static AppController app(RentManager manager, InputReader input, PrintStream output) {
        return new AppController(input, new MainMenuView(output),
                new UnitController(manager,input,new UnitMenuView(output)),
                new TenantController(manager,input,new TenantMenuView(output)),
                new PaymentController(manager,input,new PaymentMenuView(output)),
                new ReportController(manager,input,new ReportMenuView(output)),
                new SettingsController(manager,input,new SettingsMenuView(output)));
    }

    @Test void existingMenusKeepTheirContentInPlainConsole() {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        PrintStream out=new PrintStream(bytes);
        new MainMenuView(out).showMenu(); new UnitMenuView(out).showMenu();
        new TenantMenuView(out).showMenu(); new PaymentMenuView(out).showMenu();
        new ReportMenuView(out).showMenu(); new SettingsMenuView(out).showMenu();
        String result=bytes.toString();
        assertTrue(result.contains("===== RENT PAYMENT AND UNIT TRACKER ====="));
        assertTrue(result.contains("7. Display hash map"));
        assertTrue(result.contains("4. View payment history"));
        assertTrue(result.contains("5. View charge history"));
        assertFalse(result.contains("Utility"));
    }

    @Test void menuChangesUpdateFixedHeaderWithoutRepeatingItInOutput() {
        BlockingQueue<Message> events=new LinkedBlockingQueue<>();
        try(TuiSession session=new TuiSession(events::offer)) {
            new UnitMenuView(session.output()).showMenu();
            TuiSession.Header menu=assertInstanceOf(TuiSession.Header.class, events.remove());
            assertEquals("UNIT MANAGEMENT", menu.title());
            assertEquals(8,menu.options().length);
            assertTrue(events.isEmpty());
            session.output().println("Total units: 7");
            assertEquals("Total units: 7", assertInstanceOf(TuiSession.Output.class,events.remove()).text().trim());
        }
    }

    @Test void adapterKeepsValidationAndEnterToSkip() {
        Queue<String> answers=new LinkedList<>(List.of("bad","0","5000.505","","","-","2026-13","2026-11","","bad","09xx","09171234567"));
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        InputReader reader=new InputReader((Function<String,String>) prompt -> answers.remove(),new PrintStream(bytes));
        assertEquals(new BigDecimal("5000.51"),reader.readMoney("Rent: "));
        assertEquals(new BigDecimal("3000"),reader.readOptionalMoney("Rent: ",new BigDecimal("3000"),"Invalid rent"));
        assertEquals("Maria",reader.readOptionalName("Name: ","First name","Maria"));
        assertEquals("",reader.readOptionalMiddleName("Middle: ","Dela"));
        assertEquals(YearMonth.of(2026,11),reader.readBillingMonth("Month: ",YearMonth.of(2026,10)));
        assertEquals(30,reader.readRentalDays("Days: ",30));
        assertEquals("09171234567",reader.readContactNumber("Phone: "));
        assertTrue(bytes.toString().contains("valid billing month"));
        assertTrue(answers.isEmpty());
    }

    @Test void scannerStillTrimsAndReportsEof() {
        InputReader reader=new InputReader(new Scanner("  a-101  \n\n"),new PrintStream(OutputStream.nullOutputStream()));
        assertEquals("A-101",reader.readUnitNumber("Unit: "));
        assertEquals("",reader.readMiddleName("Middle: "));
        assertThrows(NoSuchElementException.class, () -> reader.readText("Next: "));
    }

    @Test void repeatedEnterCannotAnswerTheNextPromptAndCloseUnblocksInput() throws Exception {
        BlockingQueue<Message> events=new LinkedBlockingQueue<>();
        try(TuiSession session=new TuiSession(events::offer); ExecutorService worker=Executors.newSingleThreadExecutor()) {
            Future<String> first=worker.submit(() -> session.readLine("First: "));
            TuiSession.Prompt prompt=assertInstanceOf(TuiSession.Prompt.class,events.poll(2,TimeUnit.SECONDS));
            assertTrue(session.submit(prompt.id(),""));
            assertFalse(session.submit(prompt.id(),"duplicate"));
            assertEquals("",first.get(2,TimeUnit.SECONDS));
            Future<String> second=worker.submit(() -> session.readLine("Second: "));
            TuiSession.Prompt next=assertInstanceOf(TuiSession.Prompt.class,events.poll(2,TimeUnit.SECONDS));
            assertFalse(session.submit(prompt.id(),"old"));
            assertTrue(session.submit(next.id(),"answer"));
            assertEquals("answer",second.get(2,TimeUnit.SECONDS));
            Future<String> pending=worker.submit(() -> session.readLine("Third: "));
            assertInstanceOf(TuiSession.Prompt.class,events.poll(2,TimeUnit.SECONDS));
            session.close();
            assertInstanceOf(NoSuchElementException.class,assertThrows(ExecutionException.class,() -> pending.get(2,TimeUnit.SECONDS)).getCause());
        }
    }

    @Test void unicodeOutputSurvivesByteByByteWrites() throws Exception {
        BlockingQueue<Message> events=new LinkedBlockingQueue<>();
        try(TuiSession session=new TuiSession(events::offer)) {
            for(byte value:"Paid ₱500.00 — José\n".getBytes(java.nio.charset.StandardCharsets.UTF_8)) session.output().write(value);
            assertEquals("Paid ₱500.00 — José\n",assertInstanceOf(TuiSession.Output.class,events.remove()).text());
        }
    }

    @Test void headerPromptAndInputFitResizedTerminal() {
        HeaderView header=new HeaderView();
        header.set("PAYMENTS",new String[]{"1. Record payment by tenant ID","2. Check balance by tenant ID","3. Charge rent by tenant ID","4. View payment history","5. View charge history","0. Back"});
        ContentView content=new ContentView();
        content.append("Tenant ID: T-001\nBalance owed: PHP 2000.00\n");
        TextInput input=new TextInput(); input.setValue("2026-10"); input.cursorEnd();
        for(int width:new int[]{32,60,100}) {
            String result=plain(new ScreenView().render(header,content,"Enter billing month (YYYY-MM, Enter for 2026-10): ",new InputRowView().render(input,width-2,true),width,30));
            assertTrue(result.contains("PAYMENTS"));
            assertEquals(30,result.split("\n",-1).length);
            for(String line:result.split("\n",-1)) assertTrue(Size.width(line)<=width-1,"Too wide: "+line);
        }
    }

    @Test void inputCursorStaysInsideItsRowWhenValueIsLong() {
        TextInput input=new TextInput(); input.setValue("A".repeat(150)); input.cursorEnd();
        String result=plain(new InputRowView().render(input,30,true));
        assertTrue(Size.width(result)<=30);
        assertTrue(result.startsWith("❯ "));
    }

    @Test void viewportScrollsAndPansWithoutChangingHeaderOrInput() {
        BlockingQueue<Message> events=new LinkedBlockingQueue<>();
        try(TuiSession session=new TuiSession(events::offer)) {
            TuiView view=new TuiView(session,() -> { });
            view.update(new WindowSizeMessage(60,24));
            view.update(new TuiSession.Header("REPORTS",new String[]{"1. Display table","0. Back"}));
            for(int i=0;i<60;i++) view.update(new TuiSession.Output("ROW-"+i+" "+"X".repeat(80)+" END\n"));
            view.update(new TuiSession.Prompt(1,"Choice: "));
            String bottom=plain(view.view());
            view.update(key(KeyType.KeyPgUp));
            String previous=plain(view.view());
            assertNotEquals(bottom,previous);
            assertTrue(previous.contains("REPORTS") && previous.contains("1 display table"));
            view.update(key(KeyType.KeyCtrlRight));
            assertNotEquals(previous,plain(view.view()));
            view.update(new WindowSizeMessage(0,0));
            assertEquals(22,plain(view.view()).split("\n",-1).length);
            assertNotNull(view.update(key(KeyType.keyETX)).command());
        }
    }

    @Test void realControllersRunThroughTuiKeysWithBillingAndSkippedUpdates() throws Exception {
        RentManager manager=new RentManager(CLOCK);
        assertEquals(7,DataLoader.load(manager));
        BlockingQueue<Message> events=new LinkedBlockingQueue<>();
        String[] answers={"1","3","A-101","","","","0","2","6","0","3","3","T-001","2026-11","15","Y","1","T-001","2026-10","500","4","T-001","5","T-001","0","4","4","0","5","1","0","0"};
        try(TuiSession session=new TuiSession(events::offer); ExecutorService worker=Executors.newSingleThreadExecutor()) {
            TuiView view=new TuiView(session,() -> { });
            Future<?> controller=worker.submit(() -> app(manager,new InputReader(session::readLine,session.output()),session.output()).run());
            StringBuilder result=new StringBuilder();
            int next=0;
            while(!controller.isDone() || !events.isEmpty()) {
                Message event=events.poll(2,TimeUnit.SECONDS);
                if(event==null) { assertTrue(controller.isDone(),"Controller hung waiting for a key"); break; }
                view.update(event);
                if(event instanceof TuiSession.Output output) result.append(output.text());
                if(event instanceof TuiSession.Prompt prompt) {
                    assertTrue(next<answers.length,"Unexpected prompt: "+prompt.text());
                    view.update(text(answers[next++]));
                    view.update(key(next % 2 == 0 ? KeyType.keyLF : KeyType.keyCR));
                }
            }
            controller.get(2,TimeUnit.SECONDS);
            assertEquals(answers.length,next);
            assertEquals(new BigDecimal("5000.00"),manager.findUnit("A-101").getMonthlyRent());
            assertEquals(new BigDecimal("4000.00"),manager.findUnit("A-101").getBalance());
            assertTrue(result.toString().contains("Total tenants: 5"));
            assertTrue(result.toString().contains("P-001") && result.toString().contains("C-001"));
            assertTrue(result.toString().contains("2026-10-29") && result.toString().contains("2026-11"));
            assertEquals(1,manager.findTenantLedger("T-001").getPaymentCount());
        }
    }

    @Test void closingTuiDuringPaymentLeavesBalanceAndHistoryUntouched() throws Exception {
        RentManager manager=new RentManager(CLOCK); DataLoader.load(manager);
        BlockingQueue<Message> events=new LinkedBlockingQueue<>();
        try(TuiSession session=new TuiSession(events::offer); ExecutorService worker=Executors.newSingleThreadExecutor()) {
            Future<?> controller=worker.submit(() -> app(manager,new InputReader(session::readLine,session.output()),session.output()).run());
            String[] answers={"3","1","T-001","2026-10"}; int next=0;
            while(true) {
                Message event=events.poll(2,TimeUnit.SECONDS);
                assertNotNull(event);
                if(event instanceof TuiSession.Prompt prompt) {
                    if(next==answers.length) { session.close(); break; }
                    assertTrue(session.submit(prompt.id(),answers[next++]));
                }
            }
            controller.get(2,TimeUnit.SECONDS);
            assertEquals(new BigDecimal("2000.00"),manager.findUnit("A-101").getBalance());
            assertEquals(0,manager.findTenantLedger("T-001").getPaymentCount());
        }
    }

    @Test void sampleHeaderUsesTwoColumnsBracketedNumbersAndSeparateExit() {
        HeaderView header=new HeaderView();
        header.set("RENT PAYMENT AND UNIT TRACKER",new String[]{"1. Unit Management","2. Tenant Management","3. Payments","4. Reports","5. Table Settings","0. Exit"});
        String frame=plain(header.render(180));
        assertTrue(frame.contains("Command-driven console · powered by TUI4J\n\n"));
        String[] rows=frame.split("\n");
        String first=Arrays.stream(rows).filter(row -> row.contains("[1]")).findFirst().orElseThrow();
        assertTrue(first.contains("[2] Tenant Management"));
        assertFalse(first.contains("[3]"));
        assertEquals("[0] Exit",Arrays.stream(rows).filter(row -> row.contains("[0]")).findFirst().orElseThrow());
        assertTrue(frame.contains("\n\n" + HelpView.COMMANDS));
        String narrow=plain(header.menu(30));
        for(String row:narrow.split("\n")) assertTrue(Size.width(row)<=30);
    }

    @Test void screenshotSizedScreenKeepsHeaderTranscriptAndFooterSeparated() {
        HeaderView header=new HeaderView();
        header.set("TENANT MANAGEMENT",new String[]{"1. Assign tenant to unit","2. View tenant by ID","3. Update tenant by ID","4. Move out tenant by ID","5. List all tenants","6. Count all tenants","7. Display hash map","0. Back"});
        ContentView content=new ContentView();
        content.recordInput("Choice: ","3",header);
        content.recordInput("Enter tenant ID: ","T-001",header);
        content.append("Tenant updated successfully.\nTime: O(1). Space: O(1).\n");
        TextInput input=new TextInput();
        String frame=plain(new ScreenView().render(header,content,"Choice: ",new InputRowView().render(input,178,true,"command"),180,55));
        String[] rows=frame.split("\n",-1);
        assertEquals(55,rows.length);
        assertTrue(rows[0].startsWith(" ─"));
        assertEquals(" TENANT MANAGEMENT",rows[1]);
        assertTrue(frame.contains("❯ 3") && frame.contains("Update tenant by ID"));
        assertTrue(frame.contains("Enter tenant ID: T-001"));
        assertFalse(frame.contains("Choice: 3"));
        assertTrue(rows[rows.length-4].contains("1 assign · 2 view"));
        assertEquals(" ❯ command",rows[rows.length-3]);
        assertTrue(rows[rows.length-2].startsWith(" ─"));
        assertEquals(" " + HelpView.TIPS,rows[rows.length-1]);
        for(String row:rows) assertTrue(Size.width(row)<180);
    }

    @Test void transcriptColorsErrorsSuccessAndComplexityLikeTheSample() {
        com.williamcallahan.tui4j.compat.lipgloss.Renderer.defaultRenderer().setColorProfile(
                com.williamcallahan.tui4j.compat.lipgloss.color.ColorProfile.TrueColor);
        ContentView content=new ContentView();
        content.resize(150,8);
        content.append("Amount must be greater than 0.\nPayment recorded successfully.\nTime: O(1). Space: O(1).\n");
        String colored=content.render("");
        assertTrue(colored.contains("38;2;255;135;149"));
        assertTrue(colored.contains("38;2;158;219;158"));
        assertTrue(colored.contains("38;2;144;153;167"));
        assertTrue(plain(colored).contains("Payment recorded successfully."));
    }

    @Test void helpAndClearKeepTheCurrentInputRequestPending() throws Exception {
        BlockingQueue<Message> events=new LinkedBlockingQueue<>();
        try(TuiSession session=new TuiSession(events::offer); ExecutorService worker=Executors.newSingleThreadExecutor()) {
            TuiView view=new TuiView(session,() -> { });
            view.update(new WindowSizeMessage(180,56));
            view.update(new TuiSession.Header("PAYMENTS",new String[]{"1. Record payment by tenant ID","0. Back"}));
            Future<String> answer=worker.submit(() -> session.readLine("Enter payment amount: "));
            view.update(assertInstanceOf(TuiSession.Prompt.class,events.poll(2,TimeUnit.SECONDS)));
            view.update(text("/help")); view.update(key(KeyType.keyCR));
            assertFalse(answer.isDone());
            assertTrue(plain(view.view()).contains("MENU HELP"));
            assertTrue(plain(view.view()).contains("Enter payment amount:"));
            view.update(text("/clear")); view.update(key(KeyType.keyLF));
            assertFalse(answer.isDone());
            assertFalse(plain(view.view()).contains("MENU HELP"));
            assertTrue(plain(view.view()).contains("Enter payment amount:"));
            view.update(text("500")); view.update(key(KeyType.keyCR));
            assertEquals("500",answer.get(2,TimeUnit.SECONDS));
        }
    }

    @Test void helpListsNavigationAndCancellationCommands() {
        assertEquals("/back · /cancel · /exit · /clear",HelpView.COMMANDS);
        assertTrue(HelpView.DETAILS.contains("/back -"));
        assertTrue(HelpView.DETAILS.contains("/exit -"));
        assertTrue(HelpView.DETAILS.contains("/clear -"));
        assertTrue(HelpView.DETAILS.contains("/cancel -"));
        assertFalse(HelpView.DETAILS.contains("/help"));
        assertFalse(HelpView.DETAILS.contains("record payment"));
    }

    @Test void backCancelsPendingUnitUpdatesAndExitClosesNormally() {
        RentManager manager=new RentManager(CLOCK); DataLoader.load(manager);
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        PrintStream output=new PrintStream(bytes);
        String commands="1\n3\nA-101\nA-999\n/BACK\n2\n6\n0\n/exit\n";
        app(manager,new InputReader(new Scanner(commands),output),output).run();
        assertNotNull(manager.findUnit("A-101"));
        assertNull(manager.findUnit("A-999"));
        assertEquals(new BigDecimal("5000.00"),manager.findUnit("A-101").getMonthlyRent());
        assertTrue(bytes.toString().contains("Total tenants: 5"));
        assertTrue(bytes.toString().contains("Goodbye!"));
        assertFalse(bytes.toString().contains("Input ended"));
    }

    @Test void backFromPendingPaymentDoesNotRecordAnything() {
        RentManager manager=new RentManager(CLOCK); DataLoader.load(manager);
        PrintStream output=new PrintStream(OutputStream.nullOutputStream());
        String commands="3\n1\nT-001\n2026-10\n/back\n/back\n/EXIT\n";
        app(manager,new InputReader(new Scanner(commands),output),output).run();
        assertEquals(new BigDecimal("2000.00"),manager.findUnit("A-101").getBalance());
        assertEquals(0,manager.findTenantLedger("T-001").getPaymentCount());
    }

    private static String runCommands(RentManager manager,String commands) {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        PrintStream output=new PrintStream(bytes);
        app(manager,new InputReader(new Scanner(commands),output),output).run();
        String result=bytes.toString();
        assertTrue(result.contains("Goodbye!"));
        assertFalse(result.contains("Input ended"));
        return result;
    }

    @Test void backIsIdenticalToZeroAtEveryMenuIncludingMain() {
        for(String section:new String[]{"","1\n","2\n","3\n","4\n","5\n"}) {
            RentManager numeric=new RentManager(CLOCK), command=new RentManager(CLOCK);
            DataLoader.load(numeric); DataLoader.load(command);
            String suffix=section.isEmpty()?"":"0\n";
            assertEquals(runCommands(numeric,section+"0\n"+suffix),runCommands(command,section+"/back\n"+suffix));
        }
    }

    @Test void cancelUnitUpdateStaysInUnitSectionAndDiscardsPartialEdits() {
        RentManager manager=new RentManager(CLOCK); DataLoader.load(manager);
        String result=runCommands(manager,"1\n3\nA-101\nA-999\n/CANCEL\n6\n/back\n/exit\n");
        assertTrue(result.contains("Transaction cancelled."));
        assertTrue(result.contains("Total units: 7"));
        assertNotNull(manager.findUnit("A-101")); assertNull(manager.findUnit("A-999"));
        assertEquals(new BigDecimal("5000.00"),manager.findUnit("A-101").getMonthlyRent());
    }

    @Test void cancelTenantAssignmentAndUpdateStayInTenantSection() {
        RentManager manager=new RentManager(CLOCK); DataLoader.load(manager);
        String original=manager.findTenant("T-001").getFullName();
        String result=runCommands(manager,"2\n1\nB-201\n/cancel\n3\nT-001\nChanged\n/cancel\n6\n/back\n/exit\n");
        assertTrue(result.contains("Total tenants: 5"));
        assertTrue(manager.findUnit("B-201").isVacant());
        assertEquals(original,manager.findTenant("T-001").getFullName());
    }

    @Test void cancelPaymentAndChargeDoNotChangeBalancesOrHistory() {
        RentManager manager=new RentManager(CLOCK); DataLoader.load(manager);
        String result=runCommands(manager,"3\n1\nT-001\n2026-10\n/cancel\n3\nT-001\n2026-11\n15\n/cancel\n2\nT-001\n/back\n/exit\n");
        assertTrue(result.contains("Balance owed: PHP 2000.00"));
        assertEquals(new BigDecimal("2000.00"),manager.findUnit("A-101").getBalance());
        assertEquals(0,manager.findTenantLedger("T-001").getPaymentCount());
        assertEquals(1,manager.findTenantLedger("T-001").getChargeCount());
    }

    @Test void cancelRehashAtConfirmationLeavesBothTablesUnchanged() {
        RentManager manager=new RentManager(CLOCK); DataLoader.load(manager);
        int units=manager.getCapacity(), tenants=manager.getTenantCapacity();
        String result=runCommands(manager,"5\n2\n/cancel\n1\n/back\n/exit\n");
        assertTrue(result.contains("Unit buckets: " + units));
        assertEquals(units,manager.getCapacity()); assertEquals(tenants,manager.getTenantCapacity());
    }

    @Test void cancelAtMenusKeepsTheCurrentMenuInsteadOfExiting() {
        for(String section:new String[]{"","1\n","2\n","3\n","4\n","5\n"}) {
            RentManager manager=new RentManager(CLOCK); DataLoader.load(manager);
            String suffix=section.isEmpty()?"":"/back\n";
            String result=runCommands(manager,section+"/cancel\n"+suffix+"/exit\n");
            assertTrue(result.contains("No transaction is in progress."));
            assertEquals(7,manager.size());
        }
    }

    @Test void enteredValuesAreYellowAndActionNamesArePurple() {
        com.williamcallahan.tui4j.compat.lipgloss.Renderer.defaultRenderer().setColorProfile(
                com.williamcallahan.tui4j.compat.lipgloss.color.ColorProfile.TrueColor);
        HeaderView header=new HeaderView(); header.set("TABLE SETTINGS",new String[]{"2. Rehash now"});
        ContentView content=new ContentView(); content.resize(150,10);
        content.recordInput("Choice: ","2",header);
        content.append("This doubles the buckets in the unit table and tenant-ID index.\n");
        content.recordInput("Confirm rehash? (Y/N): ","Y",header);
        content.append("Both hash tables rehashed successfully.\n");
        String colored=content.render("");
        String action=Arrays.stream(colored.split("\n")).filter(row -> row.contains("Rehash now")).findFirst().orElseThrow();
        String answer=Arrays.stream(colored.split("\n")).filter(row -> row.contains("Confirm rehash?")).findFirst().orElseThrow();
        assertTrue(action.contains("38;2;201;167;255"));
        assertTrue(answer.contains("38;2;246;211;101"));
        assertTrue(plain(answer).stripTrailing().equals("Confirm rehash? (Y/N): Y"));
        assertTrue(colored.contains("38;2;158;219;158"));
        TextInput input=new TextInput(); input.setValue("Y"); input.cursorEnd();
        assertTrue(new InputRowView().render(input,78,true,"").contains("38;2;246;211;101"));
    }

    /** Checks rendered colors while comparing actual view text against its plain-console output. */
    private static String coloredView(java.util.function.Consumer<PrintStream> render) {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        render.accept(new PrintStream(bytes));
        BlockingQueue<Message> events=new LinkedBlockingQueue<>();
        ContentView content=new ContentView(); content.resize(2000,100);
        try(TuiSession session=new TuiSession(events::offer)) {
            render.accept(session.output());
            while(!events.isEmpty()) content.append(assertInstanceOf(TuiSession.Output.class,events.remove()).text());
        }
        String frame=content.render("");
        String expected=bytes.toString().replace("\r\n","\n").lines().map(String::stripTrailing).collect(java.util.stream.Collectors.joining("\n")).stripTrailing();
        String actual=plain(frame).lines().map(String::stripTrailing).collect(java.util.stream.Collectors.joining("\n")).stripTrailing();
        assertEquals(expected,actual);
        return frame;
    }

    @Test void everyRecordTableUsesPurpleHeadingsAndAlternatingRowsWithoutChangingData() {
        com.williamcallahan.tui4j.compat.lipgloss.Renderer.defaultRenderer().setColorProfile(
                com.williamcallahan.tui4j.compat.lipgloss.color.ColorProfile.TrueColor);
        RentManager manager=new RentManager(CLOCK); DataLoader.load(manager);
        manager.chargeRentByTenantId("T-001",YearMonth.of(2026,11),15);
        manager.recordPaymentByTenantId("T-001",YearMonth.of(2026,10),new BigDecimal("500"));
        manager.recordPaymentByTenantId("T-001",YearMonth.of(2026,10),new BigDecimal("250"));
        TenantLedger ledger=manager.findTenantLedger("T-001");
        List<java.util.function.Consumer<PrintStream>> tables=List.of(
                out -> { UnitMenuView v=new UnitMenuView(out); v.showUnitListHeader(); v.showUnitListRow(manager.findUnit("A-101")); v.showUnitListRow(manager.findUnit("A-102")); v.showUnitCount(7); },
                out -> { TenantMenuView v=new TenantMenuView(out); v.showTenantListHeader(); v.showTenantListRow("A-101",manager.findTenant("T-001")); v.showTenantListRow("A-102",manager.findTenant("T-002")); v.showTenantCount(5); },
                out -> { PaymentMenuView v=new PaymentMenuView(out); v.showPaymentHistoryHeader("T-001",ledger.getTenantName()); ledger.forEachPayment(v::showPaymentHistoryRow); v.showPaymentHistoryTotals(ledger.getPaymentCount(),ledger.getTotalPaid()); },
                out -> { PaymentMenuView v=new PaymentMenuView(out); v.showChargeHistoryHeader("T-001",ledger.getTenantName()); ledger.forEachCharge(v::showChargeHistoryRow); v.showChargeHistoryTotals(ledger.getChargeCount(),ledger.getTotalCharged()); });
        for(var table:tables) {
            String frame=coloredView(table);
            String[] rows=frame.split("\n");
            List<String> tableRows=Arrays.stream(rows).filter(row -> row.contains(" | ")).toList();
            assertTrue(tableRows.get(0).contains("38;2;201;167;255"));
            assertTrue(tableRows.get(1).contains("38;2;142;220;230"));
            assertTrue(tableRows.get(2).contains("38;2;241;244;248"));
        }
    }

    @Test void detailRecordsAndHashMapsUseColoredLabelsAndValuesWithoutChangingText() {
        RentManager manager=new RentManager(CLOCK); DataLoader.load(manager);
        String frame=coloredView(out -> {
            new UnitMenuView(out).showUnit(manager.findUnit("A-101"));
            new TenantMenuView(out).showTenant("A-101",manager.findTenant("T-001"));
            new ReportMenuView(out).showTable(manager.getTableLayout());
            new ReportMenuView(out).showTotalBalance(manager.totalBalance());
        });
        assertTrue(frame.contains("38;2;117;182;255"));
        assertTrue(frame.contains("38;2;142;220;230"));
        assertTrue(plain(frame).contains("Total balance owed: PHP 10000.00"));
    }
}
