# Bisq Security Review Checklist

Comprehensive security validation patterns for Bitcoin/cryptocurrency code in Bisq PRs.

## Critical Security Domains

### 1. Private Key Handling

**Validation Points**:
- [ ] No private keys in source code
- [ ] No keys in log statements or error messages
- [ ] No keys in exception stack traces
- [ ] Memory cleared after key usage (if applicable)
- [ ] Key derivation uses approved methods (BIP32/BIP39)

**File Patterns to Check**:
```bash
grep -E "(bitcoin|crypto|wallet|key|seed|mnemonic)" --output_mode files_with_matches
```

**Common Vulnerabilities**:
- Hardcoded keys or seeds
- Logging sensitive key material
- Insecure key storage mechanisms
- Missing key zeroization after use

### 2. Bitcoin Transaction Safety

**Validation Points**:
- [ ] Transaction fee calculation uses satoshi arithmetic (no floating point)
- [ ] Dust threshold enforcement (546 satoshis minimum output)
- [ ] Transaction size limits respected
- [ ] Input validation for amounts (no negative, no overflow)
- [ ] Proper UTXO selection logic
- [ ] Change address generation is correct
- [ ] RBF (Replace-By-Fee) handling if applicable

**Critical Checks**:
```java
// Fee calculation - must use satoshis, not BTC
long fee = (long)(txSize * feeRatePerByte); // ✅ Correct
double fee = txSize * feeRatePerBTC;        // ❌ Floating point risk

// Dust threshold
if (outputValue < 546) { /* reject */ }     // ✅ Correct

// Overflow protection
long total = amount1 + amount2;             // ❌ Can overflow
long total = Math.addExact(amount1, amount2); // ✅ Throws on overflow
```

**Common Vulnerabilities**:
- Floating-point arithmetic in fee calculations
- Missing dust threshold checks
- Integer overflow in amount addition
- Incorrect change calculation
- Missing transaction validation

### 3. Cryptographic Operations

**Validation Points**:
- [ ] Using approved algorithms (Ed25519, secp256k1)
- [ ] No deprecated crypto (MD5, SHA1 for security)
- [ ] Proper random number generation (SecureRandom)
- [ ] No hardcoded IVs or salts
- [ ] Signature verification before trust
- [ ] Constant-time comparison for secrets

**Approved Algorithms**:
- **Signatures**: Ed25519, ECDSA (secp256k1)
- **Hashing**: SHA-256, SHA-512, RIPEMD-160
- **Encryption**: AES-GCM, ChaCha20-Poly1305
- **Key Derivation**: PBKDF2, scrypt, Argon2

**Common Vulnerabilities**:
```java
// Random number generation
Random rng = new Random();              // ❌ Predictable
SecureRandom rng = new SecureRandom();  // ✅ Cryptographically secure

// Comparison timing attacks
if (hmac.equals(expected)) { }          // ❌ Timing attack
if (MessageDigest.isEqual(hmac, expected)) { } // ✅ Constant-time
```

### 4. P2P Message Validation

**Validation Points**:
- [ ] Message size limits enforced (prevent DoS)
- [ ] Input sanitization on all peer data
- [ ] Deserialization safety (no arbitrary code execution)
- [ ] Protocol version compatibility checks
- [ ] Message signature verification
- [ ] Rate limiting on message processing

**Size Limits**:
```java
// Maximum message sizes
public static final int MAX_MESSAGE_SIZE = 1024 * 1024; // 1 MB
public static final int MAX_PEER_ADDRESSES = 1000;

if (message.length() > MAX_MESSAGE_SIZE) {
    throw new MessageSizeException();
}
```

**Common Vulnerabilities**:
- No message size validation (DoS risk)
- Trusting peer-provided data without validation
- Unsafe deserialization (object injection)
- Missing protocol version checks
- No rate limiting on expensive operations

### 5. Financial Logic Safety

**Validation Points**:
- [ ] Trade amount calculations use proper precision
- [ ] Percentage calculations avoid floating point
- [ ] Security deposit calculations correct
- [ ] Escrow amount validation
- [ ] Maker/Taker fee calculations accurate
- [ ] BSQ (Bisq token) amount handling correct

**Precision Handling**:
```java
// Use basis points for percentages (10000 = 100%)
long fee = (amount * feeRateBasisPoints) / 10000; // ✅ Integer math

// Avoid floating point
double fee = amount * 0.001; // ❌ Precision loss risk
```

**Common Vulnerabilities**:
- Floating-point precision loss in financial calculations
- Rounding errors in fee calculations
- Integer overflow in multiplication
- Division before multiplication (precision loss)
- Missing boundary checks on amounts

## Security Review Template

Use this template when reviewing security-sensitive code:

```markdown
## Bisq Security Analysis for PR #{number}

### Bitcoin Transaction Code
- **Files Changed**: {list}
- **Risk Assessment**: {CRITICAL|HIGH|MEDIUM|LOW}
- **Findings**:
  - Fee calculation: {✅|⚠️|❌} {details}
  - Dust threshold: {✅|⚠️|❌} {details}
  - Overflow protection: {✅|⚠️|❌} {details}
  - UTXO handling: {✅|⚠️|❌} {details}

### Cryptographic Operations
- **Files Changed**: {list}
- **Findings**:
  - Algorithm approval: {✅|⚠️|❌} {details}
  - Random generation: {✅|⚠️|❌} {details}
  - Signature verification: {✅|⚠️|❌} {details}

### P2P Protocol Changes
- **Files Changed**: {list}
- **Findings**:
  - Message size limits: {✅|⚠️|❌} {details}
  - Input validation: {✅|⚠️|❌} {details}
  - Deserialization safety: {✅|⚠️|❌} {details}

### Financial Logic
- **Files Changed**: {list}
- **Findings**:
  - Precision handling: {✅|⚠️|❌} {details}
  - Overflow checks: {✅|⚠️|❌} {details}
  - Fee calculations: {✅|⚠️|❌} {details}

### Overall Security Assessment
- **Critical Issues**: {count}
- **High Priority**: {count}
- **Risk Level**: {CRITICAL|HIGH|MEDIUM|LOW}
- **Recommendation**: {BLOCK MERGE|REQUEST CHANGES|APPROVE WITH NOTES|APPROVE}
```

## Risk Categorization

**CRITICAL** (Block merge immediately):
- Private key exposure
- Arbitrary code execution vulnerabilities
- Fund loss risks
- Weak cryptography in security-critical paths

**HIGH** (Must fix before merge):
- Missing overflow checks in financial code
- Insufficient input validation
- Missing message size limits
- Deprecated crypto algorithms

**MEDIUM** (Should fix):
- Suboptimal random number generation
- Missing rate limiting
- Incomplete validation logic
- Performance issues in crypto operations

**LOW** (Nice to have):
- Code style in security code
- Missing code comments
- Documentation updates
