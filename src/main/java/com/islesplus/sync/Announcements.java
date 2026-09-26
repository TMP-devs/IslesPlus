package com.islesplus.sync;

/**
 * Remembers which announcement this client has already seen, so one push shows once. The id is
 * persisted in islesplus.json: a message stays shown-once across restarts, and a brand new id
 * (see {@link Announcement}) shows again.
 */
public final class Announcements {

    /** id of the last announcement shown in chat, "" = none yet. persisted by IslesPlusConfig */
    public static volatile String lastShownId = "";

    /** issued time (ISO) of the last announcement shown, "" = none yet. persisted by IslesPlusConfig */
    public static volatile String lastShownIssued = "";

    private Announcements() {}

    /** the announcement that still needs showing, or null when there is nothing new */
    public static Announcement pending() {
        Announcement announcement = AnnouncementFeed.current();
        return announcement != null && announcement.isUnseen(lastShownId) ? announcement : null;
    }

    /** called once the message is in chat, so it does not come back next tick */
    public static void markShown(Announcement announcement) {
        lastShownId = announcement.id();
        AnnouncementEnvelope.Opened latest = AnnouncementFeed.latest();
        if (latest != null && latest.announcement() == announcement) lastShownIssued = latest.issued().toString();
    }

    /** called after the config loads: anything issued at or before the last shown one stays hidden */
    public static void restoreFeed() {
        try {
            if (!lastShownIssued.isBlank()) AnnouncementFeed.markSeen(java.time.Instant.parse(lastShownIssued.trim()));
        } catch (java.time.format.DateTimeParseException ignored) {
            // a hand-edited or broken value: fall back to the id check alone
        }
    }
}
