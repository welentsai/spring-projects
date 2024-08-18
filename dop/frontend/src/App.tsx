import * as React from 'react';
import './App.css'
import { StoryFinder } from './components/feature/StoryFinder';
import { AppLayout } from './components/laylout/AppLayout';
import { ComponentWithRefInstanceVariable } from './components/feature/ComponentWithRefInstanceVariable';
import { ComponentWithRefRead } from './components/feature/ComponentWithRefRead';
import { StoryBoard } from './components/feature/StoryBoard';

const App: React.FC = () => {

  return (
    <StoryBoard />
    // <StoryFinder />
    // <AppLayout />
    // <ComponentWithRefRead />
  );

};

export default App
