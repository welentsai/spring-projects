import * as React from 'react';
import './App.css'
import react from './assets/react.svg';

import { StoryFinder } from './components/feature/StoryFinder';
import { AppLayout } from './components/laylout/AppLayout';
import { ComponentWithRefInstanceVariable } from './components/feature/ComponentWithRefInstanceVariable';
import { ComponentWithRefRead } from './components/feature/ComponentWithRefRead';
import { StoryBoard } from './components/feature/StoryBoard';

const App: React.FC = () => {

  return (
    <div className='bg-gray-100'>
      <div className='px-8 py-12'>
        <img className='h-10' src={react} alt="React" />
        <img className='mt-10 rounded-3xl' src={react} alt="React" />
        <h1 className='mt-6 text-2xl font-bold text-gray-900'>You can work everywhere. <span className='text-indigo-500'>Take advantage of it</span></h1>
        <p className='mt-2 text-gray-500'>Workcation help you find work-friendly rentals in beautiful locations so you can enjoy some nice weather even when you're not on vocation</p>
        <div className='mt-4'>
          <a className='inline-block px-5 py-3 bg-indigo-500 text-white rounded-lg shadow-lg uppercase tracking-wider font-semibold text-sm' href='#'>Book your escape</a>
        </div>
      </div>
    </div>
    // <StoryBoard />
    // <StoryFinder />
    // <AppLayout />
    // <ComponentWithRefRead />
  );

};

export default App
