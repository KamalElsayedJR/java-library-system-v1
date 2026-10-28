
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;

public class LibrarySystem {

    public static void main(String[] args) {
        Library library = new Library();

        library.addMember(new Member(1, "Mona", MemberType.STUDENT));
        library.addMember(new Member(2, "Ahmed", MemberType.STAFF));

        try {
            library.addBook(new Book("222", "Effective Java", "Joshua Bloch"));
            library.addBook(new Book("333", "Clean Code", "Robert C. Martin"));
            library.addBook(new Book("444", "Java: The Complete Reference", "Herbert Schildt"));
        } catch (DuplicateBookException e) {
            System.out.println("Error: " + e.getMessage());
        }

        Scanner in = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.println("\n===== Library System =====");
            System.out.println("1) List books");
            System.out.println("2) Search");
            System.out.println("3) Add book");
            System.out.println("4) Borrow");
            System.out.println("5) Return");
            System.out.println("6) Overdue");
            System.out.println("0) Exit");
            System.out.print("Choice: ");

            String choice = in.nextLine().trim();

            try {
                switch (choice) {
                    case "1":
                        for (Book book : library.allBooks()) {
                            System.out.println(book);
                        }
                        break;

                    case "2":
                        System.out.print("Search title or author: ");
                        String text = in.nextLine().trim();

                        List<Book> results = library.search(text);
                        if (results.isEmpty()) {
                            System.out.println("No matching books found.");
                        } else {
                            results.forEach(System.out::println);
                        }
                        break;

                    case "3":
                        System.out.print("ISBN: ");
                        String isbn = in.nextLine().trim();

                        System.out.print("Title: ");
                        String title = in.nextLine().trim();

                        System.out.print("Author: ");
                        String author = in.nextLine().trim();

                        library.addBook(new Book(isbn, title, author));
                        System.out.println("Book added successfully.");
                        break;

                    case "4":
                        System.out.print("ISBN: ");
                        String borrowIsbn = in.nextLine().trim();

                        System.out.print("Member id: ");
                        int memberId = Integer.parseInt(in.nextLine().trim());

                        Loan loan = library.borrow(
                                borrowIsbn, memberId, LocalDate.now()
                        );

                        System.out.println("Borrowed: " + loan);
                        break;

                    case "5":
                        System.out.print("ISBN: ");
                        String returnIsbn = in.nextLine().trim();

                        library.returnBook(returnIsbn);
                        System.out.println("Book returned successfully.");
                        break;

                    case "6":
                        List<Loan> overdueLoans =
                                library.overdue(LocalDate.now());

                        if (overdueLoans.isEmpty()) {
                            System.out.println("No overdue loans.");
                        } else {
                            overdueLoans.forEach(System.out::println);
                        }
                        break;

                    case "0":
                        running = false;
                        System.out.println("Goodbye!");
                        break;

                    default:
                        System.out.println("Invalid choice. Try again.");
                }
            } catch (LibraryException | NumberFormatException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }

        in.close();
    }
}

// ==================== Exceptions ====================

class LibraryException extends Exception {
    public LibraryException(String message) {
        super(message);
    }
}

class BookNotFoundException extends LibraryException {
    public BookNotFoundException(String message) {
        super(message);
    }
}

class MemberNotFoundException extends LibraryException {
    public MemberNotFoundException(String message) {
        super(message);
    }
}

class BookUnavailableException extends LibraryException {
    public BookUnavailableException(String message) {
        super(message);
    }
}

class LoanLimitException extends LibraryException {
    public LoanLimitException(String message) {
        super(message);
    }
}

class DuplicateBookException extends LibraryException {
    public DuplicateBookException(String message) {
        super(message);
    }
}

// ==================== Library ====================

class Library {
    private final Map<String, Book> books = new HashMap<>();
    private final Map<Integer, Member> members = new HashMap<>();
    private final List<Loan> loans = new ArrayList<>();

    public void addBook(Book book) throws DuplicateBookException {
        if (books.containsKey(book.getIsbn())) {
            throw new DuplicateBookException(
                    "Book with ISBN " + book.getIsbn() + " already exists"
            );
        }
        books.put(book.getIsbn(), book);
    }

    public void addMember(Member member) {
        members.put(member.getId(), member);
    }

    public Book findBook(String isbn) throws BookNotFoundException {
        Book book = books.get(isbn);

        if (book == null) {
            throw new BookNotFoundException("No book with ISBN " + isbn);
        }

        return book;
    }

    public Member findMember(int id) throws MemberNotFoundException {
        Member member = members.get(id);

        if (member == null) {
            throw new MemberNotFoundException(
                    "No member with id " + id
            );
        }

        return member;
    }

    public List<Book> allBooks() {
        List<Book> result = new ArrayList<>(books.values());
        result.sort(Comparator.comparing(
                Book::getTitle,
                String.CASE_INSENSITIVE_ORDER
        ));
        return result;
    }

    public List<Book> search(String text) {
        String query = text.toLowerCase();

        List<Book> result = new ArrayList<>();

        for (Book book : books.values()) {
            if (book.getTitle().toLowerCase().contains(query)
                    || book.getAuthor().toLowerCase().contains(query)) {
                result.add(book);
            }
        }

        result.sort(Comparator.comparing(
                Book::getTitle,
                String.CASE_INSENSITIVE_ORDER
        ));

        return result;
    }

    public int activeLoans(Member member) {
        int count = 0;

        for (Loan loan : loans) {
            if (!loan.isReturned()
                    && loan.getMember().equals(member)) {
                count++;
            }
        }

        return count;
    }

    public Loan borrow(String isbn, int memberId, LocalDate today)
            throws LibraryException {

        Book book = findBook(isbn);
        Member member = findMember(memberId);

        if (!book.isAvailable()) {
            throw new BookUnavailableException(
                    book.getTitle() + " is already borrowed"
            );
        }

        if (!member.canBorrow(activeLoans(member))) {
            throw new LoanLimitException(
                    member.getName() + " has reached the loan limit"
            );
        }

        Loan loan = new Loan(book, member, today);
        loans.add(loan);

        return loan;
    }

    public void returnBook(String isbn) throws LibraryException {
        findBook(isbn);

        for (int i = loans.size() - 1; i >= 0; i--) {
            Loan loan = loans.get(i);

            if (loan.getBook().getIsbn().equals(isbn)
                    && !loan.isReturned()) {
                loan.returnBook();
                return;
            }
        }

        throw new BookUnavailableException(
                "No active loan found for ISBN " + isbn
        );
    }

    public List<Loan> overdue(LocalDate today) {
        List<Loan> result = new ArrayList<>();

        for (Loan loan : loans) {
            if (loan.isOverdue(today)) {
                result.add(loan);
            }
        }

        return result;
    }
}

// ==================== Domain Model ====================

enum BookStatus {
    AVAILABLE,
    BORROWED
}

enum MemberType {
    STUDENT(2, 14),
    STAFF(5, 30);

    private final int maxLoans;
    private final int loanDays;

    MemberType(int maxLoans, int loanDays) {
        this.maxLoans = maxLoans;
        this.loanDays = loanDays;
    }

    public int maxLoans() {
        return maxLoans;
    }

    public int loanDays() {
        return loanDays;
    }
}

class Book {
    private final String isbn;
    private final String title;
    private final String author;
    private BookStatus bookStatus = BookStatus.AVAILABLE;

    public Book(String isbn, String title, String author) {
        this.isbn = isbn;
        this.title = title;
        this.author = author;
    }

    public String getIsbn() {
        return isbn;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public boolean isAvailable() {
        return bookStatus == BookStatus.AVAILABLE;
    }

    public void markBorrowed() {
        bookStatus = BookStatus.BORROWED;
    }

    public void markReturned() {
        bookStatus = BookStatus.AVAILABLE;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Book)) {
            return false;
        }

        Book other = (Book) obj;
        return Objects.equals(isbn, other.isbn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isbn);
    }

    @Override
    public String toString() {
        return String.format(
                "%s by %s [%s]", title, author, bookStatus
        );
    }
}

class Member {
    private final int id;
    private final String name;
    private final MemberType type;

    public Member(int id, String name, MemberType type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public MemberType getType() {
        return type;
    }

    public boolean canBorrow(int currentLoans) {
        return currentLoans < type.maxLoans();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Member)) {
            return false;
        }

        Member other = (Member) obj;
        return id == other.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format(
                "#%d %s (%s, max %d books)",
                id, name, type, type.maxLoans()
        );
    }
}

class Loan {
    private final Book book;
    private final Member member;
    private final LocalDate borrowedOn;
    private final LocalDate dueOn;
    private boolean returned;

    public Loan(Book book, Member member, LocalDate borrowedOn) {
        this.book = book;
        this.member = member;
        this.borrowedOn = borrowedOn;
        this.dueOn = borrowedOn.plusDays(member.getType().loanDays());
        this.returned = false;

        book.markBorrowed();
    }

    public Book getBook() {
        return book;
    }

    public Member getMember() {
        return member;
    }

    public LocalDate getBorrowedOn() {
        return borrowedOn;
    }

    public LocalDate getDueOn() {
        return dueOn;
    }

    public boolean isReturned() {
        return returned;
    }

    public boolean isOverdue(LocalDate today) {
        return !returned && today.isAfter(dueOn);
    }

    public void returnBook() {
        if (!returned) {
            returned = true;
            book.markReturned();
        }
    }

    @Override
    public String toString() {
        return book.getTitle() + " -> "
                + member.getName() + ", due " + dueOn;
    }
}
