package model;

public class DashboardStats {
    public int totalBooks;
    public int totalMembers;
    public int totalBorrowed;
    public int totalOverdue;

    public DashboardStats(int b, int m, int br, int o) {
        this.totalBooks = b;
        this.totalMembers = m;
        this.totalBorrowed = br;
        this.totalOverdue = o;
    }
}