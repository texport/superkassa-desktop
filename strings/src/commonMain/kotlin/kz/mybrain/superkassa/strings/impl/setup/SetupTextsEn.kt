package kz.mybrain.superkassa.strings.impl.setup

import kz.mybrain.superkassa.strings.api.setup.SetupTexts

/** Надписи [SetupTexts] по-английски. */
internal val setupTextsEn = SetupTexts(
    explain = "The steps run in order; the wizard can be closed and resumed later",
    startOver = "Start over",
    startOverAsk = "Forget what the wizard has done?",
    startOverExplain = "The factory number and the register created in the cabinet stay there — the wizard " +
        "forgets them, and the connection starts with a new number",
    stepFactory = "Factory number",
    stepFactoryHint = "The register issues the number and it is remembered: a second request would give another",
    getFactory = "Get the number",
    stepCabinet = "The register in the BFD cabinet",
    stepCabinetHint = "Sign in with the owner's certificate, then create the register with this factory number",
    signInFirst = "Sign in to the cabinet first",
    addedToCabinet = "Created in the cabinet",
    stepApplication = "Registration",
    stepApplicationHint = "The application is signed and sent to the KGD; the answer does not come at once",
    submit = "Submit the application",
    registered = "The register is on record",
    stepAdmin = "The register at this workplace and the admin PIN",
    stepAdminHint = "The token is issued by the cabinet and never written to a file: resume later and a new one is issued",
    connect = "Create the register",
    connected = "The register is connected — you can sign in",
    done = "done",
    waiting = "waiting for the previous step",
    viaCabinet = "Through the cabinet",
    manually = "By hand",
    manuallyHint = "When someone else created the register in the BFD: you already have the identifier and the token",
    status = "Register status:",
    notStored = "The register was not created: the BFD did not confirm it. " +
        "Check the connection to the BFD and try again",
    noToken = "The cabinet has not issued the register token yet: registration in ISNA is not finished. " +
        "Wait for the ISNA answer and try again",
    pinRepeat = "Admin PIN again",
    pinsDiffer = "The PINs do not match"
)
