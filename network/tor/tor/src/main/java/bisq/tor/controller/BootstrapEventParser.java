package bisq.tor.controller;

import bisq.tor.controller.events.events.BootstrapEvent;

import java.util.Optional;

public class BootstrapEventParser {
    public static Optional<BootstrapEvent> tryParse(String[] parts) {
        // 650 STATUS_CLIENT NOTICE BOOTSTRAP PROGRESS=50 TAG=loading_descriptors SUMMARY="Loading relay descriptors"
        if (isBootstrapEvent(parts)) {
            BootstrapEvent bootstrapEvent = parseBootstrapEvent(parts);
            return Optional.of(bootstrapEvent);
        }

        return Optional.empty();
    }

    private static boolean isBootstrapEvent(String[] parts) {
        // 650 STATUS_CLIENT NOTICE BOOTSTRAP PROGRESS=50 TAG=loading_descriptors SUMMARY="Loading relay descriptors"
        return parts.length >= 7 && parts[3].equals("BOOTSTRAP");
    }


    private static BootstrapEvent parseBootstrapEvent(String[] parts) {
        String progress = parts[4].replace("PROGRESS=", "");
        String tag = parts[5].replace("TAG=", "");
        String summary = parseBootstrapSummary(parts);

        int progressInt = Integer.parseInt(progress);
        return new BootstrapEvent(progressInt, tag, summary);
    }

    private static String parseBootstrapSummary(String[] parts) {
        StringBuilder summary = new StringBuilder();

        // SUMMARY="Loading relay descriptors" has whitespaces in string
        for (int i = 6; i < parts.length; i++) {
            String summaryPart = parts[i];
            summary.append(summaryPart)
                    .append(" ");
        }

        String summaryPrefix = "SUMMARY=\"";
        summary.delete(0, summaryPrefix.length());

        // ends with `" `
        int length = summary.length();
        summary.delete(length - 2, length);

        return summary.toString();
    }
}
