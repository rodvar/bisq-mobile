# Security Policy

  ## Supported Versions

  Bisq is security-sensitive software: vulnerabilities may affect user funds,
  privacy, trade settlement, or network integrity. Security fixes are prioritized
  for the current release line and active hotfix branches.
  
### Bisq Easy (mobile)

| Version | Supported          | Comments |
| ------- | ------------------ | ---------- |
| 0.3.x   | :white_check_mark: |  | 
| 0.2.x   | :white_check_mark: | To be discontinued when bisq core 2.1.8 gets enforced |
| < 0.1.x | :x:                |  |

### Connect apps (Android & iOS)

| Version | Supported          | Comments |
| ------- | ------------------ | ---------- |
| 0.1.x   | :white_check_mark: | To be discontinued when bisq connect 0.2.0 gets released |

Users should run the latest official Bisq release from
  https://bisq.network/downloads and verify release signatures. Do not downgrade
  Bisq unless maintainers explicitly instruct you to do so.

  ## Reporting a Vulnerability

  Please do **not** report security vulnerabilities through public GitHub issues,
  pull requests, Discussions, Matrix rooms, forums, or social media.

  Report suspected vulnerabilities privately through GitHub's **Report a
  vulnerability** flow on this repository's Security page. If that option is not
  available, open a minimal public issue asking maintainers to enable a private
  security reporting channel, but do not include exploit details.

  Include as much detail as possible:

  - affected version, branch, commit, or release tag;
  - affected component, such as trade protocol, wallet, P2P networking, DAO,
    seednodes, price feeds, updater, or build/release infrastructure;
  - clear impact assessment, especially whether user funds, privacy, signatures,
    settlement, or network availability are affected;
  - reproduction steps, logs, screenshots, transaction IDs, or packet/message
    traces where relevant;
  - proof-of-concept code only when needed to demonstrate impact;
  - whether the issue is already exploited or time-sensitive.

  Bisq is an open-source project maintained by contributors. Response times may
  vary, but reports involving active exploitation, possible loss of funds, privacy
  exposure, or trade-protocol compromise are treated as urgent critical bugs and
  will be triaged as quickly as possible.

  For lower-severity issues, maintainers will respond when contributor capacity is
  available.

  If the report is accepted, maintainers may coordinate a fix in a private fork or
  security advisory, prepare release and network-mitigation steps, and publish an
  advisory after users have had a reasonable opportunity to update. If the report
  is declined, maintainers will explain the reason when possible.

  Please give maintainers reasonable time to investigate and release mitigations
  before public disclosure. For severe or actively exploited issues, coordinate
  timing with maintainers so public details do not increase harm to users.

  Bisq does not currently guarantee a bug bounty. Security work that qualifies as
  a critical bug fix may be eligible for Bisq DAO compensation according to the
  project's contributor and critical-bug processes.
