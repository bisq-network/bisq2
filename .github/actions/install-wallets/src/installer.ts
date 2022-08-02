export interface Installer {
    programName: string
    installDir: string
    install(): void;
}