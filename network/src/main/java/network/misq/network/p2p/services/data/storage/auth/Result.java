/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package network.misq.network.p2p.services.data.storage.auth;

import lombok.Getter;

@Getter
public class Result {
    private final boolean success;
    private boolean publicKeyInvalid, sequenceNrInvalid, signatureInvalid,
            dataInvalid, expired, noEntry, alreadyRemoved;

    public Result(boolean success) {
        this.success = success;
    }

    public Result publicKeyInvalid() {
        publicKeyInvalid = true;
        return this;
    }

    public Result sequenceNrInvalid() {
        sequenceNrInvalid = true;
        return this;
    }

    public Result signatureInvalid() {
        signatureInvalid = true;
        return this;
    }

    public Result expired() {
        expired = true;
        return this;
    }

    public Result dataInvalid() {
        dataInvalid = true;
        return this;
    }


    public Result noEntry() {
        noEntry = true;
        return this;
    }

    public Result alreadyRemoved() {
        alreadyRemoved = true;
        return this;
    }

    @Override
    public String toString() {
        return "Result{" +
                "\r\n     success=" + success +
                ",\r\n     publicKeyInvalid=" + publicKeyInvalid +
                ",\r\n     sequenceNrInvalid=" + sequenceNrInvalid +
                ",\r\n     signatureInvalid=" + signatureInvalid +
                ",\r\n     dataInvalid=" + dataInvalid +
                ",\r\n     expired=" + expired +
                ",\r\n     noEntry=" + noEntry +
                ",\r\n     alreadyRemoved=" + alreadyRemoved +
                "\r\n}";
    }
}
