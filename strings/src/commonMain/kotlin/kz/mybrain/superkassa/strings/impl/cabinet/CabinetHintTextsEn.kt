package kz.mybrain.superkassa.strings.impl.cabinet

import kz.mybrain.superkassa.strings.api.cabinet.CabinetHintTexts

/** Надписи [CabinetHintTexts] по-английски. */
internal val cabinetHintTextsEn = CabinetHintTexts(
    signIn = "NCALayer asks for the signature: certificate and password are entered in its own window",
    signWait = "NCALayer opens the signing window. It may have appeared behind the main window — " +
        "look for it among the windows. If there is none, stop waiting and sign in again",
    signNoAnswer = "NCALayer accepted the request but returned no signature: it is busy or never showed " +
        "the signing window. Check the NCALayer windows and sign in again",
    address = "The cabinet is a separate service with its own address",
    cardVersionsEmpty = "A version appears on re-registration and on deregistration",
    placesEmpty = "A place is the address where a register stands. Until there is one, no register can be added",
    registersEmpty = "Create a register, then file a KGD application to put it on record",
    okedsEmpty = "The primary activity code goes into the registration application — set at least one",
    chooseRegister = "The company registers are on the left: choose one and its card opens here",
    documentsEmpty = "Whatever the BFD accepted from this register appears here",
    documentsNoneInPeriod = "The register has documents from another time — widen the period or take the whole time",
    actionsEmpty = "Every application leaves a trace here: when it was filed and how it ended",
    pickRegisterFirst = "Places and their registers are on the left: pick one and it opens here",
    technicalUnknown = "The state appears after the register first contacts the BFD",
    token = "The key the register signs its requests with. Copy it into the register settings: it is not shown here " +
        "twice",
    internalName = "A note to yourself: it is not sent to the BFD",
    receipts = "Click a row to open the receipt contents",
    cardMissing = "The card appears once the register is put on record",
    okedSearch = "A code or part of a name; an empty line shows the start of the classifier",
    technicalState = "Three parties speak about the register: the register on this machine, the KGD cabinet and " +
        "the BFD — the fiscal data base. The answer comes first, and below it who exactly said so. They " +
        "disagree when the state went stale somewhere.",
    bfdNoAnswer = "The state was not received — reread the register card",
    addressStep = "The address is chosen step by step: region, locality, street, building. " +
        "Pick from the list or type the beginning of a name as the registry spells it: " +
        "Қабанбай батыр, not Кабанбай",
    okedManual = "The cabinet does not serve the classifier: " +
        "enter the code and the name exactly as written in the OKED classifier",
    placeNotFound = "Change the query or clear the search",
    registers = "Every cash register created in the cabinet, not only the ones " +
        "running on this machine. A register is created here first, then filed for the " +
        "record, and the KGD assigns it a registration number",
    places = "A retail place is the address where a register stands: it goes into the " +
        "KGD application and is printed on the receipt. The coordinates are for the map — " +
        "they show where the place is and where its registers connect from",
    placesTree = "The retail places of the company on the left, each with its " +
        "registers under it: a register lives in a place, and without a place it cannot " +
        "be created. The search covers the place name, the register name and its number",
    okeds = "The activity types are what the company does, taken from the OKED " +
        "classifier (NC RK 03-2019); the cabinet holds the classifier, so a type is " +
        "picked from the list rather than typed in your own words. There is exactly one " +
        "primary type: it goes into the application, and without it the KGD will not put " +
        "a register on record",
    passport = "The passport is how the register is recorded at the KGD: registration " +
        "number, factory number, model and retail place. It also shows whether this " +
        "register runs on this machine, and lets you edit what can still be edited",
    onThisMachine = "The registers in the cabinet and the registers on this computer " +
        "are different lists: a register can be on record and still run in another shop. " +
        "This says whether a cashier can stand behind it here and now",
    receiptCard = "The receipt in full: items, payment, taxes and who rang it up. The " +
        "KGD mark is what the receipt is opened for: it means the receipt reached the BFD " +
        "and was accepted",
    reportCard = "The register report for a shift: revenue, refunds and the cash in " +
        "the drawer. The numbers must match the tape the cashier holds — a mismatch means " +
        "not everything reached the BFD",
    shiftCard = "A shift is one register's working day, from opening to the Z report. " +
        "The totals appear after it closes: an open shift has none yet, and the empty " +
        "lines here are not a fault",
    cashMovement = "A deposit or a withdrawal is money the cashier put into the " +
        "drawer or took out of it without selling anything. It is not revenue, but it " +
        "changes the cash in the register, and without it the drawer will not reconcile",
    factoryNumber = "The number the manufacturer stamped on the register: it goes " +
        "into the application and stands in the registration card. While the register " +
        "is not on record it can be fixed here",
    card = "The registration card is what the KGD recorded about the register: " +
        "number, model, retail place and the date it went on record. A new card version " +
        "appears with every re-registration, and the versions show what exactly changed",
    actionsJournal = "The trail of every application to the KGD: which one was filed, " +
        "when, and how it ended. This is where you look to see why the register is in " +
        "its current state, or to prove that an application was filed",
    newPlaceNotChosen = "Re-registration moves the register to another retail place — " +
        "choose it in the list above, otherwise the cabinet will refuse the application"
)
