package bisq.tor.controller;

import bisq.tor.controller.events.events.*;

import java.util.Optional;

public class HsDescEventParser {
    public static Optional<HsDescEvent> tryParse(String[] parts) {
        if (HsDescEvent.Action.CREATED.isAction(parts)) {
            // 650 HS_DESC CREATED <onion_address> UNKNOWN UNKNOWN <descriptor_id>
            HsDescCreatedOrReceivedEvent hsDescEvent = HsDescCreatedOrReceivedEvent.builder()
                    .action(HsDescEvent.Action.CREATED)
                    .hsAddress(parts[3])
                    .authType(parts[4])
                    .hsDir(parts[5])
                    .descriptorId(parts[6])
                    .build();
            return Optional.of(hsDescEvent);

        } else if (HsDescEvent.Action.UPLOAD.isAction(parts)) {
            // 650 HS_DESC UPLOAD <onion_address> UNKNOWN <hs_dir> <descriptor_id> HSDIR_INDEX=<index>
            HsDescUploadEvent hsDescEvent = HsDescUploadEvent.builder()
                    .action(HsDescEvent.Action.UPLOAD)
                    .hsAddress(parts[3])
                    .authType(parts[4])
                    .hsDir(parts[5])
                    .descriptorId(parts[6])
                    .hsDirIndex(parts[7])
                    .build();
            return Optional.of(hsDescEvent);

        } else if (HsDescEvent.Action.UPLOADED.isAction(parts)) {
            // 650 HS_DESC UPLOADED <onion_address> UNKNOWN <hs_dir>
            HsDescUploadedEventV2 hsDescEvent = HsDescUploadedEventV2.builder()
                    .action(HsDescEvent.Action.UPLOADED)
                    .hsAddress(parts[3])
                    .authType(parts[4])
                    .hsDir(parts[5])
                    .build();
            return Optional.of(hsDescEvent);

        } else if (HsDescEvent.Action.RECEIVED.isAction(parts)) {
            // 650 HS_DESC RECEIVED <onion_address> <auth_type> <hs_dir> <descriptor_id>
            HsDescCreatedOrReceivedEvent hsDescEvent = HsDescCreatedOrReceivedEvent.builder()
                    .action(HsDescEvent.Action.RECEIVED)
                    .hsAddress(parts[3])
                    .authType(parts[4])
                    .hsDir(parts[5])
                    .descriptorId(parts[6])
                    .build();
            return Optional.of(hsDescEvent);

        } else if (HsDescEvent.Action.FAILED.isAction(parts)) {
            // 650 HS_DESC FAILED <onion_address> <auth_type> <hs_dir> <descriptor_id> REASON=NOT_FOUND
            HsDescCreatedOrReceivedEvent hsDescEvent = HsDescFailedEvent.builder()
                    .action(HsDescEvent.Action.FAILED)
                    .hsAddress(parts[3])
                    .authType(parts[4])
                    .hsDir(parts[5])
                    .descriptorId(parts[6])
                    .reason(parts[7])
                    .build();
            return Optional.of(hsDescEvent);

        } else {
            return Optional.empty();
        }
    }
}
