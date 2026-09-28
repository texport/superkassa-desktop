package kz.mybrain.superkassa.presentation.cabinet

import kz.mybrain.superkassa.domain.cabinet.port.CabinetPorts
import kz.mybrain.superkassa.domain.cabinet.usecase.applications.CloseShiftHere
import kz.mybrain.superkassa.domain.cabinet.usecase.applications.SubmitApplication
import kz.mybrain.superkassa.domain.cabinet.usecase.applications.SyncServiceInfoHere
import kz.mybrain.superkassa.domain.cabinet.usecase.card.ReadCardVersion
import kz.mybrain.superkassa.domain.cabinet.usecase.card.ReadCardVersions
import kz.mybrain.superkassa.domain.cabinet.usecase.card.ReadRegistrationCard
import kz.mybrain.superkassa.domain.cabinet.usecase.card.SaveCardPdf
import kz.mybrain.superkassa.domain.cabinet.usecase.company.ReadCompany
import kz.mybrain.superkassa.domain.cabinet.usecase.company.ReadOked
import kz.mybrain.superkassa.domain.cabinet.usecase.company.SaveOkeds
import kz.mybrain.superkassa.domain.cabinet.usecase.company.SearchOkeds
import kz.mybrain.superkassa.domain.cabinet.usecase.documents.OpenDocument
import kz.mybrain.superkassa.domain.cabinet.usecase.documents.ReadDocumentsOverview
import kz.mybrain.superkassa.domain.cabinet.usecase.documents.ReadDocumentsPage
import kz.mybrain.superkassa.domain.cabinet.usecase.places.AddPlace
import kz.mybrain.superkassa.domain.cabinet.usecase.places.AddRegister
import kz.mybrain.superkassa.domain.cabinet.usecase.places.HasNestedLocalities
import kz.mybrain.superkassa.domain.cabinet.usecase.places.IssueFactoryNumber
import kz.mybrain.superkassa.domain.cabinet.usecase.places.LookUpAddress
import kz.mybrain.superkassa.domain.cabinet.usecase.places.MovePlace
import kz.mybrain.superkassa.domain.cabinet.usecase.places.NameKkmFromCabinet
import kz.mybrain.superkassa.domain.cabinet.usecase.places.ReadBlockedRegisters
import kz.mybrain.superkassa.domain.cabinet.usecase.places.ReadKkmModels
import kz.mybrain.superkassa.domain.cabinet.usecase.places.ReadPlaces
import kz.mybrain.superkassa.domain.cabinet.usecase.places.ReadRegisters
import kz.mybrain.superkassa.domain.cabinet.usecase.places.RemovePlace
import kz.mybrain.superkassa.domain.cabinet.usecase.places.RenamePlace
import kz.mybrain.superkassa.domain.cabinet.usecase.places.ResolveAddress
import kz.mybrain.superkassa.domain.cabinet.usecase.places.SearchPlaces
import kz.mybrain.superkassa.domain.cabinet.usecase.register.EnrollKkmHere
import kz.mybrain.superkassa.domain.cabinet.usecase.register.IssueToken
import kz.mybrain.superkassa.domain.cabinet.usecase.register.ReadKkmStates
import kz.mybrain.superkassa.domain.cabinet.usecase.register.ReadKkmsHere
import kz.mybrain.superkassa.domain.cabinet.usecase.register.ReadOfdEnvironments
import kz.mybrain.superkassa.domain.cabinet.usecase.register.ReadRegisterCard
import kz.mybrain.superkassa.domain.cabinet.usecase.register.ReadRegisterState
import kz.mybrain.superkassa.domain.cabinet.usecase.register.ReadRegistrationActions
import kz.mybrain.superkassa.domain.cabinet.usecase.register.RemoveRegister
import kz.mybrain.superkassa.domain.cabinet.usecase.register.RenameRegister
import kz.mybrain.superkassa.domain.cabinet.usecase.register.RestampRegister
import kz.mybrain.superkassa.domain.cabinet.usecase.register.WorkOnKkm
import kz.mybrain.superkassa.domain.cabinet.usecase.register.WriteTokenHere
import kz.mybrain.superkassa.domain.cabinet.usecase.signin.ReadCabinetAddress
import kz.mybrain.superkassa.domain.cabinet.usecase.signin.SignInToCabinet
import kz.mybrain.superkassa.domain.cabinet.usecase.signin.SignOutOfCabinet
import kz.mybrain.superkassa.domain.cabinet.usecase.signin.WatchCabinetOwner
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.domain.signin.usecase.ObserveSignIn
import kz.mybrain.superkassa.domain.workplace.port.WorkplaceMemory
import kz.mybrain.superkassa.domain.workplace.usecase.ReadLocalName
import kz.mybrain.superkassa.presentation.cabinet.signing.SigningCases

/**
 * Сценарии кабинета, собранные над его портами и кассой процесса.
 *
 * Модели кабинета зовут только их: ни портов, ни кассы модель сама
 * не касается. Собираются один раз на окно — вместе с моделью кабинета.
 */
class CabinetCases(kassa: Kassa, signIn: SignIn, memory: WorkplaceMemory, ports: CabinetPorts) {
    val signIn = SignInToCabinet(ports.account)
    val signOut = SignOutOfCabinet(ports.account)
    val owner = WatchCabinetOwner(ports.account)
    val address = ReadCabinetAddress(ports.account)

    val readPlaces = ReadPlaces(ports.places)
    val searchPlaces = SearchPlaces(ports.places)
    val readRegisters = ReadRegisters(ports.registers)
    val readBlocked = ReadBlockedRegisters(ports.registers)
    val nameKkm = NameKkmFromCabinet(kassa, signIn)
    val addPlace = AddPlace(ports.places)
    val renamePlace = RenamePlace(ports.places)
    val movePlace = MovePlace(ports.places)
    val removePlace = RemovePlace(ports.places)
    val lookUpAddress = LookUpAddress(ports.addresses)
    val hasNested = HasNestedLocalities(ports.addresses)
    val resolveAddress = ResolveAddress(ports.addresses)
    val readModels = ReadKkmModels(ports.registers)
    val addRegister = AddRegister(ports.registers)
    val issueFactoryNumber = IssueFactoryNumber(kassa)

    val readCard = ReadRegisterCard(ports.registers)
    val readState = ReadRegisterState(ports.registers)
    val readActions = ReadRegistrationActions(ports.applications)
    val readKkmsHere = ReadKkmsHere(kassa)
    val readLocalName = ReadLocalName(memory)
    val observe = ObserveSignIn(signIn)
    val renameRegister = RenameRegister(ports.registers)
    val restampRegister = RestampRegister(ports.registers)
    val removeRegister = RemoveRegister(ports.registers)
    val issueToken = IssueToken(ports.registers)
    val writeToken = WriteTokenHere(kassa, signIn)
    val enrollKkm = EnrollKkmHere(kassa, ports.company)
    val workOn = WorkOnKkm(signIn, memory)
    val readEnvironments = ReadOfdEnvironments(kassa)
    val readKkmStates = ReadKkmStates(kassa)

    val readRegistrationCard = ReadRegistrationCard(ports.cards)
    val readCardVersions = ReadCardVersions(ports.cards)
    val readCardVersion = ReadCardVersion(ports.cards)
    val saveCardPdf = SaveCardPdf(ports.cards, ports.files)

    val submitApplication = SubmitApplication(ports.applications, ports.signer)
    val signing = SigningCases(ports.signing)
    val closeShift = CloseShiftHere(kassa)
    val syncServiceInfo = SyncServiceInfoHere(kassa, signIn)

    val readOverview = ReadDocumentsOverview(ports.documents)
    val readDocuments = ReadDocumentsPage(ports.documents)
    val openDocument = OpenDocument(ports.documents)

    val readCompany = ReadCompany(ports.company)
    val saveOkeds = SaveOkeds(ports.company)
    val searchOkeds = SearchOkeds(ports.company)
    val readOked = ReadOked(ports.company)
}
