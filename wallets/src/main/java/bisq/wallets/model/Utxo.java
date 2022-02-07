package bisq.wallets.model;

public record Utxo(String txId, String address, double amount, int confirmations, boolean reused) {
    public static class Builder {
        private String txId;
        private String address;
        private double amount;
        private int confirmations;
        private boolean reused;

        public Utxo build() {
            return new Utxo(
                    txId,
                    address,
                    amount,
                    confirmations,
                    reused
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

        public Builder reused(boolean reused) {
            this.reused = reused;
            return this;
        }
    }
}
