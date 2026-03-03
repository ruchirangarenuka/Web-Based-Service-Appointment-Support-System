package com.example.support;

import java.util.List;
import java.util.Scanner;

public class Main {

    private static final String ADMIN_PASSWORD = "admin123";

    public static void main(String[] args) {
        SupportSystem supportSystem = new SupportSystem();
        Scanner scanner = new Scanner(System.in);

        boolean running = true;
        while (running) {
            System.out.println("\n===== Customer Support & Feedback Management =====");
            System.out.println("1. Customer");
            System.out.println("2. Admin");
            System.out.println("0. Exit");
            System.out.print("Choose role: ");

            String choice = scanner.nextLine();
            switch (choice) {
                case "1" -> customerMenu(scanner, supportSystem);
                case "2" -> adminMenu(scanner, supportSystem);
                case "0" -> {
                    running = false;
                    System.out.println("Exiting system. Goodbye!");
                }
                default -> System.out.println("Invalid choice. Try again.");
            }
        }

        scanner.close();
    }

    private static void customerMenu(Scanner scanner, SupportSystem supportSystem) {
        System.out.print("\nEnter your name: ");
        String name = scanner.nextLine().trim();

        boolean back = false;
        while (!back) {
            System.out.println("\n--- Customer Menu (" + name + ") ---");
            System.out.println("1. Create support ticket / complaint");
            System.out.println("2. Give rating & feedback for a ticket");
            System.out.println("3. Check status of my tickets");
            System.out.println("0. Back to main menu");
            System.out.print("Choose option: ");

            String choice = scanner.nextLine();
            switch (choice) {
                case "1" -> createTicket(scanner, supportSystem, name);
                case "2" -> giveFeedback(scanner, supportSystem);
                case "3" -> viewCustomerTickets(supportSystem, name);
                case "0" -> back = true;
                default -> System.out.println("Invalid choice. Try again.");
            }
        }
    }

    private static void createTicket(Scanner scanner, SupportSystem supportSystem, String customerName) {
        System.out.print("Subject: ");
        String subject = scanner.nextLine();
        System.out.print("Description / Complaint: ");
        String description = scanner.nextLine();

        Ticket ticket = supportSystem.createTicket(customerName, subject, description);
        System.out.println("Ticket created successfully! Your ticket ID is: " + ticket.getId());
    }

    private static void giveFeedback(Scanner scanner, SupportSystem supportSystem) {
        try {
            System.out.print("Enter ticket ID: ");
            int ticketId = Integer.parseInt(scanner.nextLine());
            System.out.print("Rating (1-5): ");
            int rating = Integer.parseInt(scanner.nextLine());
            if (rating < 1 || rating > 5) {
                System.out.println("Rating must be between 1 and 5.");
                return;
            }
            System.out.print("Comments: ");
            String comments = scanner.nextLine();

            boolean ok = supportSystem.addFeedbackToTicket(ticketId, rating, comments);
            if (ok) {
                System.out.println("Thank you! Your feedback has been recorded.");
            } else {
                System.out.println("Ticket not found.");
            }
        } catch (NumberFormatException ex) {
            System.out.println("Invalid number format.");
        }
    }

    private static void viewCustomerTickets(SupportSystem supportSystem, String customerName) {
        List<Ticket> tickets = supportSystem.findTicketsByCustomer(customerName);
        if (tickets.isEmpty()) {
            System.out.println("You have no tickets.");
            return;
        }

        System.out.println("\nYour tickets:");
        for (Ticket t : tickets) {
            System.out.println(t);
        }
    }

    private static void adminMenu(Scanner scanner, SupportSystem supportSystem) {
        System.out.print("\nEnter admin password: ");
        String pwd = scanner.nextLine();
        if (!ADMIN_PASSWORD.equals(pwd)) {
            System.out.println("Incorrect password.");
            return;
        }

        boolean back = false;
        while (!back) {
            System.out.println("\n--- Admin Menu ---");
            System.out.println("1. View all tickets");
            System.out.println("2. Reply to ticket");
            System.out.println("3. Update ticket status");
            System.out.println("4. Close ticket (mark as CLOSED)");
            System.out.println("0. Back to main menu");
            System.out.print("Choose option: ");

            String choice = scanner.nextLine();
            switch (choice) {
                case "1" -> viewAllTickets(supportSystem);
                case "2" -> replyToTicket(scanner, supportSystem);
                case "3" -> updateTicketStatus(scanner, supportSystem);
                case "4" -> closeTicket(scanner, supportSystem);
                case "0" -> back = true;
                default -> System.out.println("Invalid choice. Try again.");
            }
        }
    }

    private static void viewAllTickets(SupportSystem supportSystem) {
        List<Ticket> tickets = supportSystem.getAllTickets();
        if (tickets.isEmpty()) {
            System.out.println("No tickets found.");
            return;
        }
        System.out.println("\nAll tickets:");
        for (Ticket t : tickets) {
            System.out.println(t);
        }
    }

    private static void replyToTicket(Scanner scanner, SupportSystem supportSystem) {
        try {
            System.out.print("Enter ticket ID: ");
            int ticketId = Integer.parseInt(scanner.nextLine());
            System.out.print("Reply message: ");
            String reply = scanner.nextLine();

            boolean ok = supportSystem.replyToTicket(ticketId, reply);
            if (ok) {
                System.out.println("Reply saved.");
            } else {
                System.out.println("Ticket not found.");
            }
        } catch (NumberFormatException ex) {
            System.out.println("Invalid number format.");
        }
    }

    private static void updateTicketStatus(Scanner scanner, SupportSystem supportSystem) {
        try {
            System.out.print("Enter ticket ID: ");
            int ticketId = Integer.parseInt(scanner.nextLine());

            System.out.println("Choose new status:");
            System.out.println("1. OPEN");
            System.out.println("2. IN_PROGRESS");
            System.out.println("3. RESOLVED");
            System.out.println("4. CLOSED");
            System.out.print("Status: ");
            String statusChoice = scanner.nextLine();

            TicketStatus status;
            switch (statusChoice) {
                case "1" -> status = TicketStatus.OPEN;
                case "2" -> status = TicketStatus.IN_PROGRESS;
                case "3" -> status = TicketStatus.RESOLVED;
                case "4" -> status = TicketStatus.CLOSED;
                default -> {
                    System.out.println("Invalid status choice.");
                    return;
                }
            }

            boolean ok = supportSystem.updateTicketStatus(ticketId, status);
            if (ok) {
                System.out.println("Status updated.");
            } else {
                System.out.println("Ticket not found.");
            }
        } catch (NumberFormatException ex) {
            System.out.println("Invalid number format.");
        }
    }

    private static void closeTicket(Scanner scanner, SupportSystem supportSystem) {
        try {
            System.out.print("Enter ticket ID: ");
            int ticketId = Integer.parseInt(scanner.nextLine());
            boolean ok = supportSystem.updateTicketStatus(ticketId, TicketStatus.CLOSED);
            if (ok) {
                System.out.println("Ticket closed.");
            } else {
                System.out.println("Ticket not found.");
            }
        } catch (NumberFormatException ex) {
            System.out.println("Invalid number format.");
        }
    }
}

