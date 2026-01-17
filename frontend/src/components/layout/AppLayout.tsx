import { Sidebar } from './Sidebar';
import { ChatRoom } from '@/components/chat/ChatRoom';

export function AppLayout() {
  return (
    <div className="h-screen flex">
      <Sidebar />
      <ChatRoom />
    </div>
  );
}
