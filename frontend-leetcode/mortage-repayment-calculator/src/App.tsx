import { useState } from 'react'
import './App.css'
import { CurrencyInput } from './components/CurrencyInput'
import { CenterLayout } from './layout/CenterLayout/component';
import { TwoColumnLayout } from './layout/TwoColumnLayout';

function App() {
  const [amount, setAmount] = useState(500)

  function handleAmountChange(amount: number): void {
    console.log('new amount', amount);
    setAmount(amount);
  }

  return (
    <>
      <div className="flex justify-center items-center min-h-screen bg-blue-50">
        <div className="bg-white rounded-lg shadow-lg overflow-hidden max-w-4xl w-full">
          <div className="flex">
            {/* Left column */}
            <div className="w-1/2 p-4 bg-gray-50">
              <div className='flex justify-between'>
                <h2 className='text-xl'>Mortgage Calculator</h2>
                <p className='p-2'>Clear all</p>
              </div>
              <div className='bg-gray-50'>
                <p>Mortgage Account</p>
                <CurrencyInput amount={amount} handleAmountChange={handleAmountChange} />
              </div>

            </div>
            {/* Right column */}
            <div className="w-1/2 bg-blue-800 p-4 text-white">
            </div>
          </div>
        </div>
      </div>
      {/* <CenterLayout>
        <TwoColumnLayout>
          <div>
            <div className='  bg-gray-50'>
              <h2 className='p-4 text-xl'>Mortgage Calculator</h2>
              <p className='p-4'>Clear all</p>
            </div>  
            <div className='bg-gray-50'>
              <p>Mortgage Account</p>
              <CurrencyInput amount={amount} handleAmountChange={handleAmountChange} />
            </div>
          </div>
          <div className='w-96'>
            <div className='bg-blue-800'>
              <h2 className='p-4 text-white text-base'>Your results</h2>
            </div>
          </div>
        </TwoColumnLayout >
      </CenterLayout > */}
    </>
  )
}

export default App
