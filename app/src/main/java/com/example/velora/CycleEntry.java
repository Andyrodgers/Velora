package com.example.velora;

public class CycleEntry {
    private long startDate;
    private long endDate;
    private boolean isStressed;
    private boolean isIll;
    private boolean hasTraveled;

    public CycleEntry(long startDate, long endDate, boolean isStressed, boolean isIll, boolean hasTraveled) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.isStressed = isStressed;
        this.isIll = isIll;
        this.hasTraveled = hasTraveled;
    }

    public long getStartDate() { return startDate; }
    public void setStartDate(long startDate) { this.startDate = startDate; }

    public long getEndDate() { return endDate; }
    public void setEndDate(long endDate) { this.endDate = endDate; }

    public boolean isStressed() { return isStressed; }
    public void setStressed(boolean stressed) { isStressed = stressed; }

    public boolean isIll() { return isIll; }
    public void setIll(boolean ill) { isIll = ill; }

    public boolean hasTraveled() { return hasTraveled; }
    public void setHasTraveled(boolean hasTraveled) { this.hasTraveled = hasTraveled; }

    public boolean isAnomalous() {
        return isStressed || isIll || hasTraveled;
    }
}
