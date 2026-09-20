# Seeded scenarios

Every scenario is engineered into `V2__seed_data.sql`. The deterministic outcome below is **ground truth**:
in Part 4 and Part 8 the agent is scored against exactly these outcomes.

| ID | Cardholder | What they would say | Transaction(s) | Correct outcome | Why it's interesting for the agent |
|----|------------|---------------------|----------------|-----------------|-----------------------------------|
| S1 | Priya (CH-1001) | "I got charged twice at that furniture place last week" | TXN-100102, TXN-100103 | File **DR-101** on the later charge, citing the earlier one | Must *find* the duplicate pair itself — the form made the human do this |
| S2 | Priya | "I cancelled StreamFlix ages ago but they're still billing me" | TXN-100104…106 | File **DR-107** on TXN-100106 only; the older charges pre-date cancellation | Must ask *when* she cancelled, and dispute only charges after that date |
| S3 | Priya | "SkyHop never refunded my cancelled flight" | TXN-100107 | **Reject**: filing window expired | Must explain a rejection kindly, not argue with the rule |
| S4 | Arjun (CH-1002) | "My QuickCart order never showed up" | TXN-100201 | File **DR-104** with an expected delivery date | Must pick the right one of several QuickCart charges |
| S5 | Arjun | "QuickCart owes me for the other order too" | TXN-100202 (+ refund 100203) | **Do not file**: already refunded in full | Should check refunds *before* trying to file |
| S6 | Arjun | "What's this UrbanNest charge from yesterday?" | TXN-100205 (PENDING) | **Cannot dispute yet**; explain pending vs posted | Must not file on a pending authorisation |
| S7 | Meera (CH-1003) | "I don't recognise SQ *TPR HSPTLTY" | TXN-100301 | **Do not file fraud**: it's Tapri Tea House, a café | The legacy form *accepts* this as fraud. Only understanding catches it |
| S8 | Meera | "Two game charges in dollars I never made" | TXN-100302, TXN-100303 | File **DR-201** on both; fraud queue | Two disputes from one message; foreign merchant, rapid repeat |
| S9 | Karan (CH-1004) | "Charged by FitZone after I cancelled" | TXN-100404 | Filing is allowed; history (3 lost disputes) should be surfaced to the analyst, **not** used to refuse | Memory and profile (Part 6) — and fairness: history informs, it doesn't decide |
