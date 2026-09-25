package kz.mybrain.superkassa.strings.impl.setup

import kz.mybrain.superkassa.strings.api.setup.SetupTexts

/** Надписи [SetupTexts] по-английски. */
internal val setupTextsEn = SetupTexts(
    stepOf = "Step %1\$s of %2\$s",
    next = "Next",
    back = "Back",
    startOver = "Start over",
    startOverAsk = "Forget what the wizard has done?",
    startOverExplain = "The factory number and the register created in the cabinet stay there — the wizard " +
        "forgets them, and the connection starts with a new number",
    stepWay = "How to connect the register",
    wayExplain = "A new register is created in the BFD and put on record with the KGD. Choose whether you do " +
        "it yourself or someone has already done it for you. You can close the wizard at any step — " +
        "the progress is kept.",
    viaCabinet = "Through the BFD cabinet",
    viaCabinetHint = "You create the register in the cabinet and apply to the KGD yourself. " +
        "The owner's digital signature is needed",
    manually = "By hand",
    manuallyHint = "A service centre or an accountant created the register in the BFD, and you have " +
        "the register identifier and the token",
    stepFactory = "Factory number",
    factoryExplain = "The register issues its own factory number — the cabinet creates the register under it. " +
        "The number is remembered: close the wizard and come back, and it stays the same.",
    factoryExplainManual = "If the register is not in the BFD yet, get the number and pass it to whoever " +
        "creates it. Already have the identifier and the token? Press Next.",
    getFactory = "Get the number",
    factoryYear = "Made in %s",
    stepCabinet = "The register in the BFD cabinet",
    cabinetExplain = "Sign in to the BFD cabinet with the owner's digital signature and create the register " +
        "there: the factory number is filled in for you. No point of sale yet? Add it in the same form.",
    signInFirst = "Sign in to the cabinet first",
    addedToCabinet = "Created in the cabinet",
    stepApplication = "Registration with the KGD",
    applicationExplain = "The cabinet prepares the application, you sign it, and it goes to the KGD. " +
        "The answer takes a while — the wizard checks for it itself. You can close the wizard and come back later.",
    submit = "Submit the application",
    registered = "The register is on record",
    status = "Register status:",
    stepCredentials = "Identifier and token",
    credentialsExplain = "The BFD issues them when the register is created. The token is the key the register " +
        "signs its requests with: the wizard does not remember it, and after a break it has to be typed again.",
    stepAdmin = "Register admin",
    adminExplain = "Set the admin PIN — you will sign in to the register with it. There is no default PIN, " +
        "so type it twice. The cabinet issues the register token itself at this moment — nothing to copy.",
    adminExplainManual = "Set the admin PIN — you will sign in to the register with it. There is no default PIN, " +
        "so type it twice.",
    connect = "Create the register",
    connected = "The register is connected — you can sign in",
    done = "done",
    notStored = "The register was not created: the BFD did not confirm it. " +
        "Check the connection to the BFD and try again",
    noToken = "The cabinet has not issued the register token yet: registration with the KGD is not finished. " +
        "Wait for the KGD answer and try again",
    pinRepeat = "Admin PIN again",
    pinsDiffer = "The PINs do not match"
)
