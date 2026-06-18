---
title: HIRS Operational Flow
---

# Operational Flow

HIRS checks system integrity through a multi-stage validation service. This section provides
a detailed breakdown of the process so that the users can understand the order of operations
and how each measurement and artifact is cryptographically verified. By establishing this
chain of trust, the system helps prevent malicious actors from injecting counterfeit hardware,
malware, or altered artifacts into the environment. 

The full process includes the following: 

1. [Manufacturer creates device](op1-manufacture.md)
2. [User deploys HIRS](op2-hirs-setup.md)
3. [UEFI captures boot info](op3-boot-capture.md)
4. [HIRS performs validation](op4-validation.md)