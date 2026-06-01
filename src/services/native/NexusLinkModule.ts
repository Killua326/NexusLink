import { NativeModules } from 'react-native';

interface NexusLinkModuleInterface {
  startServer(port: number): Promise<string>;
  stopServer(): Promise<string>;
}

const { NexusLinkModule } = NativeModules;

export default NexusLinkModule as NexusLinkModuleInterface;