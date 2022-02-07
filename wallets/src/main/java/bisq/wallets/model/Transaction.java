package bisq.wallets.model;

public record Transaction(String txId, String address, double amount, int confirmations) {
    public static class Builder {
        private String txId;
        private String address;
        private double amount;
        private int confirmations;

        public Transaction build() {
            return new Transaction(
                    txId,
                    address,
                    amount,
                    confirmations
            );
        }

        public Builder txId(String txId) {
            this.txId = txId;
            return this;
        }

        public Builder address(String address) {
            this.address = address;
            return this;
        }

        public Builder amount(double amount) {
            this.amount = amount;
            return this;
        }

        public Builder confirmations(int confirmations) {
            this.confirmations = confirmations;
            return this;
        }
    }
}
